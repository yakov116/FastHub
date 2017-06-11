package com.fastaccess.ui.modules.profile.repos;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.fastaccess.data.dao.NameParser;
import com.fastaccess.data.dao.model.FilterOptionsModel;
import com.fastaccess.data.dao.model.Login;
import com.fastaccess.data.dao.model.Repo;
import com.fastaccess.helper.RxHelper;
import com.fastaccess.provider.rest.RestProvider;
import com.fastaccess.ui.base.mvp.presenter.BasePresenter;
import com.fastaccess.ui.modules.repos.RepoPagerActivity;

import java.util.ArrayList;

/**
 * Created by Kosh on 03 Dec 2016, 3:48 PM
 */

class ProfileReposPresenter extends BasePresenter<ProfileReposMvp.View> implements ProfileReposMvp.Presenter {

    private ArrayList<Repo> repos = new ArrayList<>();
    private int page;
    private int previousTotal;
    private String username;
    private int lastPage = Integer.MAX_VALUE;
    private String currentLoggedIn;
    private FilterOptionsModel filterOptions = new FilterOptionsModel();

    @Override public int getCurrentPage() {
        return page;
    }

    @Override public int getPreviousTotal() {
        return previousTotal;
    }

    @Override public void setCurrentPage(int page) {
        this.page = page;
    }

    @Override public void setPreviousTotal(int previousTotal) {
        this.previousTotal = previousTotal;
    }

    @Override public void onError(@NonNull Throwable throwable) {
        sendToView(view -> {
            if (view.getLoadMore().getParameter() != null) {
                onWorkOffline(view.getLoadMore().getParameter());
            }
        });
        super.onError(throwable);
    }

    @Override public void onCallApi(int page, @Nullable String parameter) {
        if (currentLoggedIn == null) {
            currentLoggedIn = Login.getUser().getLogin();
        }
        if (parameter == null) {
            throw new NullPointerException("Username is null");
        }
        username = parameter;
        if (page == 1) {
            lastPage = Integer.MAX_VALUE;
            sendToView(view -> view.getLoadMore().reset());
        }
        setCurrentPage(page);
        if (page > lastPage || lastPage == 0) {
            sendToView(ProfileReposMvp.View::hideProgress);
            return;
        }
        filterOptions.setIsPersonalProfile(TextUtils.equals(currentLoggedIn, username));
        makeRestCall(TextUtils.equals(currentLoggedIn, username)
                     ? RestProvider.getUserService().getRepos(filterOptions.getQueryMap(), page)
                     : RestProvider.getUserService().getRepos(parameter, filterOptions.getQueryMap(), page),
                repoModelPageable -> {
                    lastPage = repoModelPageable.getLast();
                    if (getCurrentPage() == 1) {
                        manageObservable(Repo.saveMyRepos(repoModelPageable.getItems(), parameter));
                    }
                    sendToView(view -> view.onNotifyAdapter(repoModelPageable.getItems(), page));
                });
    }

    @NonNull @Override public ArrayList<Repo> getRepos() {
        return repos;
    }

    @Override public void onWorkOffline(@NonNull String login) {
        if (repos.isEmpty()) {
            manageDisposable(RxHelper.getObserver(Repo.getMyRepos(login).toObservable()).subscribe(repoModels ->
                    sendToView(view -> view.onNotifyAdapter(repoModels, 1))));
        } else {
            sendToView(ProfileReposMvp.View::hideProgress);
        }
    }

    @Override public void onItemClick(int position, View v, Repo item) {
        RepoPagerActivity.startRepoPager(v.getContext(), new NameParser(item.getHtmlUrl()));
    }

    @Override public void onItemLongClick(int position, View v, Repo item) {}

    public FilterOptionsModel getFilterOptions() {
        return filterOptions;
    }

    @Override
    public void onFilterApply() {
        onCallApi(1, username);
    }

    @Override
    public void onTypeSelected(String selectedType) {
        filterOptions.setType(selectedType);
    }

    @Override
    public void onSortOptionSelected(String selectedSortOption) {
        filterOptions.setSort(selectedSortOption);
    }

    @Override
    public void onSortDirectionSelected(String selectedSortDirection) {
        filterOptions.setSortDirection(selectedSortDirection);
    }
}
