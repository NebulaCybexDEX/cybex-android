package com.cybex.eto.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cybex.basemodule.event.Event;
import com.cybex.eto.R;
import com.cybex.eto.activity.details.EtoDetailsActivity;
import com.cybex.eto.activity.record.EtoRecordActivity;
import com.cybex.eto.adapter.EtoRecyclerViewAdapter;
import com.cybex.eto.base.EtoBaseFragment;
import com.cybex.provider.http.entity.EtoBanner;
import com.cybex.provider.http.entity.EtoProject;
import com.cybex.provider.http.entity.EtoProjectStatus;
import com.squareup.picasso.Picasso;
import com.youth.banner.Banner;
import com.youth.banner.listener.OnBannerListener;
import com.youth.banner.loader.ImageLoader;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.cybex.basemodule.constant.Constant.INTENT_PARAM_ETO_PROJECT_DETAILS;

public class EtoFragment extends EtoBaseFragment implements EtoMvpView,
        Toolbar.OnMenuItemClickListener,
        EtoRecyclerViewAdapter.OnItemClickListener {

    @Inject
    EtoPresenter<EtoMvpView> mEtoPresenter;

    private RecyclerView mEtoRv;
    private Toolbar mToolbar;

    private EtoRecyclerViewAdapter mEtoRecyclerViewAdapter;

    private Unbinder mUnbinder;
    private int mCallCount;

    public static EtoFragment getInstance(){
        EtoFragment etoFragment = new EtoFragment();
        return etoFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        etoActivityComponent().inject(this);
        mEtoPresenter.attachView(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_eto, container, false);
        initView(view);
        mUnbinder = ButterKnife.bind(this, view);
        mToolbar.inflateMenu(R.menu.menu_eto_record);
        mToolbar.setOnMenuItemClickListener(this);
        return view;
    }

    private void initView(View view){
        mEtoRv = view.findViewById(R.id.eto_rv);
        mToolbar = view.findViewById(R.id.toolbar);
        mEtoRv.getItemAnimator().setChangeDuration(0);
        mEtoRv.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showLoadDialog(true);
        mEtoPresenter.loadEtoProjects();
        mEtoPresenter.loadEtoBanner();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEtoPresenter.detachView();
    }

    @Override
    public void onNetWorkStateChanged(boolean isAvailable) {

    }

    @Override
    public void onLoadEtoProjects(List<EtoProject> etoProjects) {
        if (mCallCount <= etoProjects.size()) {
            for (EtoProject etoProject : etoProjects) {
                if (etoProject.getStatus().equals(EtoProject.Status.OK) ||
                        etoProject.getStatus().equals(EtoProject.Status.PRE)) {
                    mEtoPresenter.refreshProjectStatusOk(etoProject);
                }
            }
            mCallCount ++;
        }
        hideLoadDialog();
        if (mEtoRecyclerViewAdapter == null) {
            mEtoRecyclerViewAdapter = new EtoRecyclerViewAdapter(getContext(), etoProjects, null);
            mEtoRecyclerViewAdapter.setOnItemClickListener(this);
            mEtoRv.setAdapter(mEtoRecyclerViewAdapter);
        } else {
            mEtoRecyclerViewAdapter.setData(etoProjects);
        }
    }

    @Override
    public void onLoadEtoBanners(List<EtoBanner> etoBanners) {
        if(etoBanners == null || etoBanners.size() == 0){
            return;
        }
        hideLoadDialog();
        if(mEtoRecyclerViewAdapter == null){
            mEtoRecyclerViewAdapter = new EtoRecyclerViewAdapter(getContext(), null, etoBanners);
            mEtoRecyclerViewAdapter.setOnItemClickListener(this);
            mEtoRv.setAdapter(mEtoRecyclerViewAdapter);
        } else {
            mEtoRecyclerViewAdapter.setHeaderData(etoBanners);
        }
    }

    @Override
    public void onRefreshEtoProjectStatus(EtoProject etoProject) {
        mEtoRecyclerViewAdapter.notifyProjectItem(etoProject);
        EventBus.getDefault().post(new Event.OnRefreshEtoProject(etoProject));
    }

    @Override
    public void onError() {
        hideLoadDialog();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.action_eto_record){
            Intent intent = new Intent(getActivity(), EtoRecordActivity.class);
            startActivity(intent);
        }
        return false;
    }

    @Override
    public void onItemClick(EtoProject etoProject) {
        Intent intent = new Intent(getActivity(), EtoDetailsActivity.class);
        intent.putExtra(INTENT_PARAM_ETO_PROJECT_DETAILS, etoProject);
        startActivity(intent);
    }

}
