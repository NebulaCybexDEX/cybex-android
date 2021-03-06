package com.cybexmobile.activity.address;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.cybex.provider.db.DBManager;
import com.cybex.provider.db.entity.Address;
import com.cybexmobile.R;
import com.cybexmobile.adapter.TransferAccountManagerRecyclerViewAdapter;
import com.cybex.basemodule.base.BaseActivity;
import com.cybexmobile.dialog.AddressOperationSelectDialog;
import com.cybex.basemodule.dialog.CybexDialog;
import com.cybex.basemodule.toastmessage.ToastMessage;
import com.cybexmobile.shake.AntiShake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.cybex.basemodule.constant.Constant.PREF_NAME;

public class TransferAccountManagerActivity extends BaseActivity implements
        TransferAccountManagerRecyclerViewAdapter.OnItemClickListener,
        AddressOperationSelectDialog.OnAddressOperationSelectedListener {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.transfer_account_manager_rv)
    RecyclerView mRvTransferAccount;

    private String mUserName;
    private TransferAccountManagerRecyclerViewAdapter mTransferAccountAdapter;
    private Address mCurrAddress;
    private int mCurrPosition;

    private Unbinder mUnbinder;
    private Disposable mLoadAddressDisposable;
    private Disposable mDeleteAddressDisposable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_account_manager);
        mUnbinder = ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        mRvTransferAccount.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mUserName = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_NAME, "");
        mTransferAccountAdapter = new TransferAccountManagerRecyclerViewAdapter(TransferAccountManagerActivity.this, new ArrayList<>(), null);
        mTransferAccountAdapter.setOnItemClickListener(this);
        mRvTransferAccount.setAdapter(mTransferAccountAdapter);
        loadAddress();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadAddress();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mLoadAddressDisposable != null && !mLoadAddressDisposable.isDisposed()){
            mLoadAddressDisposable.dispose();
        }
        if(mDeleteAddressDisposable != null && !mDeleteAddressDisposable.isDisposed()){
            mDeleteAddressDisposable.dispose();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_transfer_account, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (AntiShake.check(item.getItemId())) { return false; }
        switch (item.getItemId()) {
            case R.id.action_add_transfer_account:
                Intent intent = new Intent(this, AddTransferAccountActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNetWorkStateChanged(boolean isAvailable) {

    }

    @Override
    public void onItemClick(Address address, int position) {
        mCurrAddress = address;
        mCurrPosition = position;
        AddressOperationSelectDialog dialog = new AddressOperationSelectDialog();
        dialog.show(getSupportFragmentManager(), AddressOperationSelectDialog.class.getSimpleName());
        dialog.setOnAddressOperationSelectedListener(this);
    }

    @Override
    public void onAddressOperationSelected(int operation) {
        if(mCurrAddress == null){
            return;
        }
        mTransferAccountAdapter.notifyItemChanged(mCurrPosition);
        switch (operation) {
            case AddressOperationSelectDialog.OPERATION_COPY:
                copyAddress();
                break;
            case AddressOperationSelectDialog.OPERATION_DELETE:
                deleteAddressConfirm();
                break;
            case AddressOperationSelectDialog.OPETATION_CANCEL:
                break;
        }
    }

    private void loadAddress(){
        if(TextUtils.isEmpty(mUserName)){
            return;
        }
        mLoadAddressDisposable = DBManager.getDbProvider(this).getAddress(mUserName, Address.TYPE_TRANSFER)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Address>>() {
                    @Override
                    public void accept(List<Address> addresses) throws Exception {
                        Collections.sort(addresses);
                        mTransferAccountAdapter.setAddresses(addresses);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    private void deleteAddressConfirm(){
        CybexDialog.showDeleteConfirmDialog(
                this,
                getResources().getString(R.string.text_delete_confirm),
                getResources().getString(R.string.text_confirm_to_delete_this_account),
                mCurrAddress,
                new CybexDialog.ConfirmationDialogClickListener() {
                    @Override
                    public void onClick(Dialog dialog) {
                        deleteAddress();
                    }
                }, null);

    }

    private void deleteAddress(){
        mDeleteAddressDisposable = DBManager.getDbProvider(this).deleteAddress(mCurrAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean result) throws Exception {
                        ToastMessage.showNotEnableDepositToastMessage(TransferAccountManagerActivity.this,
                                getResources().getString(R.string.text_deleted),
                                R.drawable.ic_check_circle_green);
                        loadAddress();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ToastMessage.showNotEnableDepositToastMessage(TransferAccountManagerActivity.this,
                                getResources().getString(R.string.text_deleted_failed),
                                R.drawable.ic_error_16px);
                    }
                });
    }

    private void copyAddress() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("address", mCurrAddress.getAddress());
        clipboard.setPrimaryClip(clip);
        ToastMessage.showNotEnableDepositToastMessage(TransferAccountManagerActivity.this,
                getResources().getString(R.string.text_copied),
                R.drawable.ic_check_circle_green);
    }

}
