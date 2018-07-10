package com.cybexmobile.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.NestedScrollView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cybexmobile.R;
import com.cybexmobile.activity.LoginActivity;
import com.cybexmobile.api.BitsharesWalletWraper;
import com.cybexmobile.api.WebSocketClient;
import com.cybexmobile.base.BaseFragment;
import com.cybexmobile.dialog.CybexDialog;
import com.cybexmobile.event.Event;
import com.cybexmobile.exception.NetworkStatusException;
import com.cybexmobile.fragment.data.WatchlistData;
import com.cybexmobile.graphene.chain.AccountBalanceObject;
import com.cybexmobile.graphene.chain.AssetObject;
import com.cybexmobile.graphene.chain.DynamicGlobalPropertyObject;
import com.cybexmobile.graphene.chain.FeeAmountObject;
import com.cybexmobile.graphene.chain.FullAccountObject;
import com.cybexmobile.graphene.chain.ObjectId;
import com.cybexmobile.graphene.chain.Operations;
import com.cybexmobile.graphene.chain.SignedTransaction;
import com.cybexmobile.utils.AssetUtil;

import org.greenrobot.eventbus.EventBus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import butterknife.Unbinder;

import static com.cybexmobile.graphene.chain.Operations.ID_CREATE_LIMIT_ORDER_OPERATION;
import static com.cybexmobile.utils.Constant.ACTION_BUY;
import static com.cybexmobile.utils.Constant.ACTION_SELL;
import static com.cybexmobile.utils.Constant.ASSET_ID_CYB;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_ACTION;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_CYB_ASSET_OBJECT;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_CYB_FEE;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_FEE;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_FULL_ACCOUNT_OBJECT;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_IS_LOGIN_IN;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_NAME;
import static com.cybexmobile.utils.Constant.BUNDLE_SAVE_WATCHLIST;
import static com.cybexmobile.utils.Constant.INTENT_PARAM_ACTION;
import static com.cybexmobile.utils.Constant.INTENT_PARAM_CYB_ASSET_OBJECT;
import static com.cybexmobile.utils.Constant.INTENT_PARAM_FEE;
import static com.cybexmobile.utils.Constant.INTENT_PARAM_FULL_ACCOUNT_OBJECT;
import static com.cybexmobile.utils.Constant.INTENT_PARAM_LOGIN_IN;
import static com.cybexmobile.utils.Constant.INTENT_PARAM_NAME;
import static com.cybexmobile.utils.Constant.INTENT_PARAM_WATCHLIST;

public class BuySellFragment extends BaseFragment {

    @BindView(R.id.buysell_btn_buy_sell)
    Button mBtnBuySell;
    @BindView(R.id.buysell_et_asset_price)
    EditText mEtAssetPrice;
    @BindView(R.id.buysell_et_asset_amount)
    EditText mEtAssetAmount;
    @BindView(R.id.buysell_tv_add)
    TextView mTvAssetAdd;
    @BindView(R.id.buysell_tv_sub)
    TextView mTvAssetSub;
    @BindView(R.id.butsell_tv_asset_rmb_price)
    TextView mTvAssetRmbPrice;
    @BindView(R.id.buysell_tv_asset_symbol)
    TextView mTvAssetSymbol;
    @BindView(R.id.buysell_tv_not_enough)
    TextView mTvNotEnough;
    @BindView(R.id.buysell_tv_percentage_25)
    TextView mTvPercentage25;
    @BindView(R.id.buysell_tv_percentage_50)
    TextView mTvPercentage50;
    @BindView(R.id.buysell_tv_percentage_75)
    TextView mTvPercentage75;
    @BindView(R.id.buysell_tv_percentage_100)
    TextView mTvPercentage100;
    @BindView(R.id.buysell_tv_asset_available)
    TextView mTvAssetAvailable;
    @BindView(R.id.buysell_tv_exchange_free)
    TextView mTvExchangeFree;
    @BindView(R.id.buysell_tv_asset_total)
    TextView mTvAssetTotal;
    @BindView(R.id.buysell_scroll_view)
    NestedScrollView mScrollView;
    @BindView(R.id.buysell_checkbox_market_trades)
    CheckBox mCbMarketTrades;
    @BindView(R.id.layout_trade_history)
    FrameLayout mLayoutTradeHistory;

    private MarketTradeHistoryFragment mMarketTradeHistoryFragment;
    private ExchangeLimitOrderFragment mExchangeLimitOrderFragment;

    private String mCurrentAction = ACTION_BUY;
    private WatchlistData mWatchlistData;
    private FullAccountObject mFullAccountObject;
    private boolean mIsLoginIn;
    private String mName;
    //交易对的手续费 买单为base 卖单为quote
    private FeeAmountObject mBaseOrQuoteExchangeFee;
    private FeeAmountObject mCybExchangeFee;
    private AssetObject mCybAssetObject;

    private Unbinder mUnbinder;

    private double mAssetRmbPrice;
    //成交额
    private double mAssetTotal;
    //已经精确的余额
    private double mBalanceAvailable;
    //cyb资产是否足够扣手续费
    private boolean mIsCybBalanceEnough;
    //交易资产是否足够
    private boolean mIsExchangeBalanceEnough;

    public static BuySellFragment getInstance(String action, WatchlistData watchlistData,
                                              FullAccountObject fullAccountObject, boolean isLoginIn, String name,
                                              FeeAmountObject fee, AssetObject cybAssetObject){
        BuySellFragment fragment = new BuySellFragment();
        Bundle bundle = new Bundle();
        bundle.putString(INTENT_PARAM_ACTION, action);
        bundle.putSerializable(INTENT_PARAM_WATCHLIST, watchlistData);
        bundle.putSerializable(INTENT_PARAM_FULL_ACCOUNT_OBJECT, fullAccountObject);
        bundle.putBoolean(INTENT_PARAM_LOGIN_IN, isLoginIn);
        bundle.putString(INTENT_PARAM_NAME, name);
        bundle.putSerializable(INTENT_PARAM_FEE, fee);
        bundle.putSerializable(INTENT_PARAM_CYB_ASSET_OBJECT, cybAssetObject);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        Bundle bundle = getArguments();
        if(bundle != null){
            mCurrentAction = bundle.getString(INTENT_PARAM_ACTION, ACTION_BUY);
            mIsLoginIn = bundle.getBoolean(INTENT_PARAM_LOGIN_IN);
            mName = bundle.getString(INTENT_PARAM_NAME);
            mWatchlistData = (WatchlistData)bundle.getSerializable(INTENT_PARAM_WATCHLIST);
            mFullAccountObject = (FullAccountObject) bundle.getSerializable(INTENT_PARAM_FULL_ACCOUNT_OBJECT);
            mBaseOrQuoteExchangeFee = (FeeAmountObject) bundle.getSerializable(INTENT_PARAM_FEE);
            mCybAssetObject = (AssetObject) bundle.getSerializable(INTENT_PARAM_CYB_ASSET_OBJECT);
        }
        if(savedInstanceState != null){
            mCurrentAction = savedInstanceState.getString(BUNDLE_SAVE_ACTION);
            mIsLoginIn = savedInstanceState.getBoolean(BUNDLE_SAVE_IS_LOGIN_IN);
            mName = savedInstanceState.getString(BUNDLE_SAVE_NAME);
            mWatchlistData = (WatchlistData) savedInstanceState.getSerializable(BUNDLE_SAVE_WATCHLIST);
            mFullAccountObject = (FullAccountObject) savedInstanceState.getSerializable(BUNDLE_SAVE_FULL_ACCOUNT_OBJECT);
            mBaseOrQuoteExchangeFee = (FeeAmountObject) savedInstanceState.getSerializable(BUNDLE_SAVE_FEE);
            mCybExchangeFee = (FeeAmountObject) savedInstanceState.getSerializable(BUNDLE_SAVE_CYB_FEE);
            mCybAssetObject = (AssetObject) savedInstanceState.getSerializable(BUNDLE_SAVE_CYB_ASSET_OBJECT);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buysell, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mEtAssetPrice.setFilters(new InputFilter[]{mPriceFilter});
        mEtAssetAmount.setFilters(new InputFilter[]{mAmountFilter});
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFragment(savedInstanceState);
        initOrResetViewData();
        initOrResetButtonData();
        initOrResetFeeData();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_SAVE_ACTION, mCurrentAction);
        outState.putBoolean(BUNDLE_SAVE_IS_LOGIN_IN, mIsLoginIn);
        outState.putString(BUNDLE_SAVE_NAME, mName);
        outState.putSerializable(BUNDLE_SAVE_WATCHLIST, mWatchlistData);
        outState.putSerializable(BUNDLE_SAVE_FULL_ACCOUNT_OBJECT, mFullAccountObject);
        outState.putSerializable(BUNDLE_SAVE_CYB_ASSET_OBJECT, mCybAssetObject);
        outState.putSerializable(BUNDLE_SAVE_FEE, mBaseOrQuoteExchangeFee);
        outState.putSerializable(BUNDLE_SAVE_CYB_FEE, mCybExchangeFee);
        FragmentManager fragmentManager = getChildFragmentManager();
        if(mMarketTradeHistoryFragment != null && mMarketTradeHistoryFragment.isAdded()){
            fragmentManager.putFragment(outState, MarketTradeHistoryFragment.class.getSimpleName(), mMarketTradeHistoryFragment);
        }
        if(mExchangeLimitOrderFragment != null && mExchangeLimitOrderFragment.isAdded()){
            fragmentManager.putFragment(outState, ExchangeLimitOrderFragment.class.getSimpleName(), mExchangeLimitOrderFragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onNetWorkStateChanged(boolean isAvailable) {

    }

    @OnFocusChange({R.id.buysell_et_asset_price, R.id.buysell_et_asset_amount})
    public void onFocusChanged(View view, boolean isFocused){
        //未登录时 获取焦点自动跳转到登录界面
        if(isFocused && !mIsLoginIn){
            view.clearFocus();
            toLogin();
        }
    }

    @OnCheckedChanged(R.id.buysell_checkbox_market_trades)
    public void onMarketTradeCheckChanged(CompoundButton button, boolean isChecked){
        mScrollView.scrollTo(0, isChecked ? mLayoutTradeHistory.getTop() : 0);
    }

    @OnClick({R.id.buysell_tv_add, R.id.buysell_tv_sub})
    public void onAssetPriceClick(View view){
        String assetPriceStr = mEtAssetPrice.getText().toString();
        if(TextUtils.isEmpty(assetPriceStr)){
            return;
        }
        double assetPrice = Double.parseDouble(assetPriceStr);
        switch (view.getId()){
            case R.id.buysell_tv_add:
                assetPrice += (1/Math.pow(10, AssetUtil.pricePrecision(mWatchlistData.getCurrentPrice())));
                break;
            case R.id.buysell_tv_sub:
                if(assetPrice > 0){
                    assetPrice -= (1/Math.pow(10, AssetUtil.pricePrecision(mWatchlistData.getCurrentPrice())));
                }
                break;
        }
        mEtAssetPrice.setText(String.format(String.format("%%.%sf", AssetUtil.pricePrecision(mWatchlistData.getCurrentPrice())), assetPrice));
    }

    @OnClick({R.id.buysell_tv_percentage_25, R.id.buysell_tv_percentage_50, R.id.buysell_tv_percentage_75, R.id.buysell_tv_percentage_100})
    public void onAssetAmountClick(View view){
        String assetPrice = mEtAssetPrice.getText().toString();
        double price = TextUtils.isEmpty(assetPrice) ? 0 : Double.parseDouble(assetPrice);
        if(price == 0){
            return;
        }
        double amount = 0;
        double fee = mBaseOrQuoteExchangeFee.amount/Math.pow(10, mCurrentAction.equals(ACTION_BUY) ?
                mWatchlistData.getBasePrecision() : mWatchlistData.getQuotePrecision());
        /**
         * fix bug:CYM-365
         * 当cyb余额足够扣手续费，交易为花费cyb时 点击100%仓位 计算数量没有减去手续费
         */
        double balanceAvailable;
        if((mCurrentAction.equals(ACTION_SELL) && mWatchlistData.getQuoteId().equals(ASSET_ID_CYB)) ||
                (mCurrentAction.equals(ACTION_BUY) && mWatchlistData.getBaseId().equals(ASSET_ID_CYB))){
            balanceAvailable = mBalanceAvailable - fee;
        } else {
            balanceAvailable = mIsCybBalanceEnough ? mBalanceAvailable : mBalanceAvailable - fee;
        }
        switch (view.getId()){
            case R.id.buysell_tv_percentage_25:
                amount = mCurrentAction.equals(ACTION_BUY) ? balanceAvailable * 0.25 / price : balanceAvailable * 0.25;
                break;
            case R.id.buysell_tv_percentage_50:
                amount = mCurrentAction.equals(ACTION_BUY) ? balanceAvailable * 0.50 / price : balanceAvailable * 0.50;
                break;
            case R.id.buysell_tv_percentage_75:
                amount = mCurrentAction.equals(ACTION_BUY) ? balanceAvailable * 0.75 / price : balanceAvailable * 0.75;
                break;
            case R.id.buysell_tv_percentage_100:
                amount = mCurrentAction.equals(ACTION_BUY) ? balanceAvailable * 1 / price : balanceAvailable * 1;
                break;
        }
        //amount 不四舍五入
        BigDecimal bigDecimal = new BigDecimal(amount).setScale(AssetUtil.amountPrecision(price), RoundingMode.DOWN);
        mEtAssetAmount.setText(String.valueOf(bigDecimal.toString()));
    }

    @OnClick(R.id.buysell_btn_buy_sell)
    public void onBtnBuySellClick(View view){
        if(!mIsLoginIn){
            toLogin();
            return;
        }
        /**
         * fix bug:CYM-401
         * 价格和数量输入框为0或空 不交易
         */
        String price = mEtAssetPrice.getText().toString();
        String amount = mEtAssetAmount.getText().toString();
        if(TextUtils.isEmpty(price) || Double.parseDouble(price) == 0 ||
                TextUtils.isEmpty(amount) || Double.parseDouble(amount) == 0){
            return;
        }
        if(mIsExchangeBalanceEnough){
            CybexDialog.showLimitOrderCreateConfirmationDialog(getContext(), mCurrentAction.equals(ACTION_BUY),
                    String.format("%s %s", price, AssetUtil.parseSymbol(mWatchlistData.getBaseSymbol())),
                    String.format("%s %s", amount, AssetUtil.parseSymbol(mWatchlistData.getQuoteSymbol())),
                    mTvAssetTotal.getText().toString(),
                    new CybexDialog.ConfirmationDialogClickListener() {
                @Override
                public void onClick(Dialog dialog) {
                    checkIfLocked(mName);
                }
            });
        }
    }

    @OnTextChanged(value = R.id.buysell_et_asset_amount, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onAmountTextChanged(Editable editable){
        calculateTotal();
    }

    @OnTextChanged(value = R.id.buysell_et_asset_price, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onPriceTextChanged(Editable editable){
        calculateTotal();
        initOrResetRmbTextData();
    }

    private void calculateTotal(){
        String assetPrice = mEtAssetPrice.getText().toString();
        String assetAmount = mEtAssetAmount.getText().toString();
        double price = TextUtils.isEmpty(assetPrice) ? 0 : Double.parseDouble(assetPrice);
        double amount = TextUtils.isEmpty(assetAmount) ? 0 : Double.parseDouble(assetAmount);
        if(price == 0 || amount == 0){
            mTvAssetTotal.setText("--");
            mTvNotEnough.setVisibility(View.INVISIBLE);
            return;
        }
        mAssetTotal = price * amount;
        mTvAssetTotal.setText(String.format(Locale.US, String.format(Locale.US, "%%.%df %%s", mWatchlistData.getBasePrecision()),
                        mAssetTotal, AssetUtil.parseSymbol(mWatchlistData.getBaseSymbol())));
        //cyb余额不足扣手续费时 需要扣除手续费

        if(mCurrentAction.equals(ACTION_BUY)){
            /**
             * BigDecimal解决double * double精度问题
             */
            double total = new BigDecimal(String.valueOf(price)).multiply(new BigDecimal(String.valueOf(amount))).doubleValue();
            if(mIsCybBalanceEnough){
                mIsExchangeBalanceEnough = mBalanceAvailable >= total;
            } else {
                mIsExchangeBalanceEnough = mBalanceAvailable - mBaseOrQuoteExchangeFee.amount/Math.pow(10, mCurrentAction.equals(ACTION_BUY) ?
                        mWatchlistData.getBasePrecision() : mWatchlistData.getQuotePrecision()) >= total;
            }
        } else {
            /**
             * fix bug:CYM-367
             * 点击100%仓位 显示余额不足
             */
            mIsExchangeBalanceEnough = mIsCybBalanceEnough && mBalanceAvailable >= amount;
        }
        mTvNotEnough.setVisibility(mIsExchangeBalanceEnough ? View.INVISIBLE : View.VISIBLE);
    }

    private void initFragment(Bundle savedInstanceState){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if(savedInstanceState != null){
            mMarketTradeHistoryFragment = (MarketTradeHistoryFragment) fragmentManager.getFragment(savedInstanceState, MarketTradeHistoryFragment.class.getSimpleName());
            mExchangeLimitOrderFragment = (ExchangeLimitOrderFragment) fragmentManager.getFragment(savedInstanceState, ExchangeLimitOrderFragment.class.getSimpleName());
        }
        if(mMarketTradeHistoryFragment == null){
            mMarketTradeHistoryFragment = MarketTradeHistoryFragment.newInstance(mWatchlistData);
        }
        if(mExchangeLimitOrderFragment == null){
            mExchangeLimitOrderFragment = ExchangeLimitOrderFragment.getInstance(mWatchlistData);
        }
        if(mExchangeLimitOrderFragment.isAdded()){
            transaction.show(mExchangeLimitOrderFragment);
        }else{
            transaction.add(R.id.layout_limit_order_container, mExchangeLimitOrderFragment, ExchangeLimitOrderFragment.class.getSimpleName());
        }
        if(mMarketTradeHistoryFragment.isAdded()){
            transaction.show(mMarketTradeHistoryFragment);
        }else{
            transaction.add(R.id.layout_trade_history_container, mMarketTradeHistoryFragment, MarketTradeHistoryFragment.class.getSimpleName());
        }
        transaction.commit();
    }

    private void initOrResetViewData(){
        if(mWatchlistData == null){
            return;
        }
        String baseSymbol = AssetUtil.parseSymbol(mWatchlistData.getBaseSymbol());
        String quoteSymbol = AssetUtil.parseSymbol(mWatchlistData.getQuoteSymbol());
        mTvAssetSymbol.setText(quoteSymbol);
        mEtAssetPrice.setText("");
        mEtAssetAmount.setText("");
        initOrResetButtonData();
        initOrResetAvailableData();
    }

    private void initOrResetAvailableData(){
        if(mFullAccountObject == null){
            mTvAssetAvailable.setText(getResources().getString(R.string.text_empty));
            return;
        }
        AccountBalanceObject accountBalanceObject = getBalance(mCurrentAction.equals(ACTION_BUY) ?
                mWatchlistData.getBaseId() : mWatchlistData.getQuoteId(), mFullAccountObject);

        if(accountBalanceObject == null || accountBalanceObject.balance == 0){
            mBalanceAvailable = 0;
            mTvAssetAvailable.setText(getResources().getString(R.string.text_empty));
            return;
        }
        mBalanceAvailable  = accountBalanceObject.balance/Math.pow(10, mCurrentAction.equals(ACTION_BUY) ?
                mWatchlistData.getBasePrecision() : mWatchlistData.getQuotePrecision());
        mTvAssetAvailable.setText(String.format(Locale.US, String.format(Locale.US, "%%.%df %%s",
                mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getBasePrecision() : mWatchlistData.getQuotePrecision()), mBalanceAvailable,
                mCurrentAction.equals(ACTION_BUY) ? AssetUtil.parseSymbol(mWatchlistData.getBaseSymbol()) : AssetUtil.parseSymbol(mWatchlistData.getQuoteSymbol())));
    }

    /**
     * 先判断是否登录 再判断cyb手续费是否足够 再判断base或者quote手续费是否足够
     */
    private void initOrResetFeeData(){
        if(mBaseOrQuoteExchangeFee == null){
            mTvExchangeFree.setText(getResources().getString(R.string.text_empty));
            return;
        }
        if(!mIsLoginIn){
            mTvExchangeFree.setText(mCybAssetObject == null ? getResources().getString(R.string.text_empty) :
                    String.format(Locale.US, String.format(Locale.US, "%%.%df %%s", mCybAssetObject.precision),
                            mBaseOrQuoteExchangeFee.amount/Math.pow(10, mCybAssetObject.precision),
                            AssetUtil.parseSymbol(mCybAssetObject.symbol)));
            return;
        }
        AccountBalanceObject accountBalanceObject = getBalance(mBaseOrQuoteExchangeFee.asset_id, mFullAccountObject);
        //先判断cyb是否足够
        if(mBaseOrQuoteExchangeFee.asset_id.equals(ASSET_ID_CYB)){
            //记录cyb手续费
            mCybExchangeFee = mBaseOrQuoteExchangeFee;
            /**
             * fix bug:CYM-380
             * 手续费显示错乱
             */
            if(accountBalanceObject != null && accountBalanceObject.balance > mBaseOrQuoteExchangeFee.amount){//cyb足够
                mIsCybBalanceEnough = true;
                mTvExchangeFree.setText(mCybAssetObject == null ? getResources().getString(R.string.text_empty) :
                        String.format(Locale.US, String.format(Locale.US, "%%.%df %%s", mCybAssetObject.precision),
                                mBaseOrQuoteExchangeFee.amount/Math.pow(10, mCybAssetObject.precision),
                                AssetUtil.parseSymbol(mCybAssetObject.symbol)));
            } else {//cyb不足
                mIsCybBalanceEnough = false;
                if((mCurrentAction.equals(ACTION_BUY) && mWatchlistData.getBaseId().equals(ASSET_ID_CYB)) ||
                        (mCurrentAction.equals(ACTION_SELL) && mWatchlistData.getQuoteId().equals(ASSET_ID_CYB))){
                    mTvExchangeFree.setText(mCybAssetObject == null ? getResources().getString(R.string.text_empty) :
                            String.format(Locale.US, String.format(Locale.US, "%%.%df %%s", mCybAssetObject.precision),
                                    mBaseOrQuoteExchangeFee.amount/Math.pow(10, mCybAssetObject.precision),
                                    AssetUtil.parseSymbol(mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getBaseSymbol() : mWatchlistData.getQuoteSymbol())));
                } else {
                    ((ExchangeFragment)getParentFragment()).loadLimitOrderCreateFee(mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getBaseId() : mWatchlistData.getQuoteId());
                }
            }
        } else {
            if((mCurrentAction.equals(ACTION_BUY) && mWatchlistData.getBaseId().equals(mBaseOrQuoteExchangeFee.asset_id)) ||
                    (mCurrentAction.equals(ACTION_SELL) && mWatchlistData.getQuoteId().equals(mBaseOrQuoteExchangeFee.asset_id))){
                if(accountBalanceObject != null && accountBalanceObject.balance > mBaseOrQuoteExchangeFee.amount){//交易对余额足够
                    mTvExchangeFree.setText(String.format(Locale.US, String.format(Locale.US, "%%.%df %%s",
                            mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getBasePrecision() : mWatchlistData.getQuotePrecision()),
                            mBaseOrQuoteExchangeFee.amount/Math.pow(10, mCurrentAction.equals(ACTION_BUY) ?
                                    mWatchlistData.getBasePrecision() : mWatchlistData.getQuotePrecision()), AssetUtil.parseSymbol(mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getBaseSymbol() : mWatchlistData.getQuoteSymbol())));
                } else {//交易对余额不足 显示cyb手续费
                    mTvExchangeFree.setText(mCybAssetObject == null ? getResources().getString(R.string.text_empty) :
                            String.format(Locale.US, String.format(Locale.US, "%%.%df %%s", mCybAssetObject.precision),
                                    mCybExchangeFee.amount/Math.pow(10, mCybAssetObject.precision), AssetUtil.parseSymbol(mCybAssetObject.symbol)));
                }
            }
        }

    }

    private void initOrResetButtonData(){
        if(mIsLoginIn){
            mBtnBuySell.setText(String.format("%s %s", getResources().getString(mCurrentAction.equals(ACTION_BUY) ?
                    R.string.text_buy : R.string.text_sell), mWatchlistData == null ? "" : AssetUtil.parseSymbol(mWatchlistData.getQuoteSymbol())));
        } else {
            mBtnBuySell.setText(getResources().getString(R.string.text_login_in_to_exchange));
        }
        mBtnBuySell.setBackgroundResource(mCurrentAction.equals(ACTION_BUY) ?
                R.drawable.bg_btn_green_gradient_enabled : R.drawable.bg_btn_red_gradient_enabled);
    }

    private void initOrResetRmbTextData(){
        String assetPrice = mEtAssetPrice.getText().toString();
        mTvAssetRmbPrice.setText(TextUtils.isEmpty(assetPrice) ? "≈¥ --" :
                String.format(Locale.US, "≈¥ %.2f", Double.parseDouble(assetPrice) * mAssetRmbPrice));
    }

    /**
     * 交易对改变
     * @param watchlist
     */
    public void changeWatchlist(WatchlistData watchlist){
        if(watchlist == null){
            return;
        }
        this.mWatchlistData = watchlist;
        if(mMarketTradeHistoryFragment != null){
            mMarketTradeHistoryFragment.changeWatchlist(mWatchlistData);
        }
        if(mExchangeLimitOrderFragment != null){
            mExchangeLimitOrderFragment.changeWatchlist(mWatchlistData);
        }
        initOrResetViewData();
    }

    public void changeFee(FeeAmountObject fee, AssetObject cybAsset){
        if(mCybAssetObject == null){
            mCybAssetObject = cybAsset;
        }
        mBaseOrQuoteExchangeFee = fee;
        initOrResetFeeData();
    }

    public void changeLoginState(boolean loginState, String name){
        mIsLoginIn = loginState;
        mName = name;
        initOrResetButtonData();
    }

    public void changeFullAccount(FullAccountObject fullAccountObject){
        mFullAccountObject = fullAccountObject;
        initOrResetAvailableData();
    }

    /**
     * 改变买入卖出价
     * @param basePrice
     */
    public void changeBuyOrSellPrice(double basePrice, double quoteAmount) {
        mEtAssetPrice.setText(String.format(AssetUtil.formatPrice(basePrice), basePrice));
        if(quoteAmount != 0){
            mEtAssetAmount.setText(String.format(AssetUtil.formatAmount(basePrice), quoteAmount));
        }
    }

    public void changeRmbPrice(double rmbPrice){
        mAssetRmbPrice = rmbPrice;
        initOrResetRmbTextData();
    }

    private AccountBalanceObject getBalance(String assetId, FullAccountObject fullAccount){
        if(assetId == null || fullAccount == null){
            return null;
        }
        List<AccountBalanceObject> accountBalances = fullAccount.balances;
        if(accountBalances == null || accountBalances.size() == 0){
            return null;
        }
        AccountBalanceObject accountBalanceObject = null;
        for(AccountBalanceObject accountBalance : accountBalances){
            if(accountBalance.asset_type.toString().equals(assetId)){
                accountBalanceObject = accountBalance;
                break;
            }
        }
        return accountBalanceObject;
    }

    /**
     * 登录
     */
    private void toLogin(){
        Intent intent = new Intent(getContext(), LoginActivity.class);
        startActivity(intent);
    }

    /**
     * 检查用户钱包状态
     * @param userName
     */
    private void checkIfLocked(String userName) {
        if(!BitsharesWalletWraper.getInstance().is_locked()){
            toExchange();
            return;
        }
        CybexDialog.showUnlockWalletDialog(getContext(), new CybexDialog.UnLockDialogClickListener() {
            @Override
            public void onClick(String password, Dialog dialog) {
                int result = BitsharesWalletWraper.getInstance().import_account_password(mFullAccountObject.account, userName, password);
                if (result == 0) {
                    dialog.dismiss();
                    toExchange();
                } else {
                    LinearLayout errorLayout = dialog.findViewById(R.id.unlock_wallet_dialog_error_layout);
                    errorLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * 挂单
     */
    private void toExchange(){
        try {
            BitsharesWalletWraper.getInstance().get_dynamic_global_properties(new WebSocketClient.MessageCallback<WebSocketClient.Reply<DynamicGlobalPropertyObject>>() {
                @Override
                public void onMessage(WebSocketClient.Reply<DynamicGlobalPropertyObject> reply) {
                    long amountSell;
                    long amountReceive;
                    if(mCurrentAction.equals(ACTION_BUY)){
                        amountSell = (long) (mAssetTotal * Math.pow(10, mWatchlistData.getBasePrecision()));
                        amountReceive = (long) (Double.parseDouble(mEtAssetAmount.getText().toString()) * Math.pow(10, mWatchlistData.getQuotePrecision()));
                    } else {
                        amountSell = (long) (Double.parseDouble(mEtAssetAmount.getText().toString()) * Math.pow(10, mWatchlistData.getQuotePrecision()));
                        amountReceive = (long) (mAssetTotal * Math.pow(10, mWatchlistData.getBasePrecision()));
                    }
                    Operations.limit_order_create_operation operation = BitsharesWalletWraper.getInstance().getLimitOrderCreateOperation(mFullAccountObject.account.id,
                            mIsCybBalanceEnough ? mCybAssetObject.id : mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getBaseAsset().id : mWatchlistData.getQuoteAsset().id,
                            mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getBaseAsset().id : mWatchlistData.getQuoteAsset().id,
                            mCurrentAction.equals(ACTION_BUY) ? mWatchlistData.getQuoteAsset().id : mWatchlistData.getBaseAsset().id,
                            mIsCybBalanceEnough ? mCybExchangeFee.amount : mBaseOrQuoteExchangeFee.amount,
                            amountSell, amountReceive);
                    SignedTransaction signedTransaction = BitsharesWalletWraper.getInstance().getSignedTransaction(
                            mFullAccountObject.account, operation, ID_CREATE_LIMIT_ORDER_OPERATION, reply.result);
                    try {
                        BitsharesWalletWraper.getInstance().broadcast_transaction_with_callback(signedTransaction, mLimitOrderCreateCallback);
                    } catch (NetworkStatusException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure() {

                }
            });
        } catch (NetworkStatusException e) {
            e.printStackTrace();
        }
    }

    public void clearEditTextData(){
        mEtAssetAmount.setText("");
        mEtAssetPrice.setText("");
    }

    private WebSocketClient.MessageCallback mLimitOrderCreateCallback = new WebSocketClient.MessageCallback<WebSocketClient.Reply<String>>(){

        @Override
        public void onMessage(WebSocketClient.Reply<String> reply) {
            EventBus.getDefault().post(new Event.LimitOrderCreate(reply.result == null && reply.error == null));
        }

        @Override
        public void onFailure() {
            EventBus.getDefault().post(new Event.LimitOrderCreate(false));
        }
    };

    private InputFilter mPriceFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if(dest.length() == 0 && source.equals(".")){
                return "0.";
            }
            String destStr = dest.toString();
            String[] destArr = destStr.split("\\.");
            if (destArr.length > 1) {
                String dotValue = destArr[1];
                if (dotValue.length() == AssetUtil.pricePrecision(mWatchlistData.getCurrentPrice())) {
                    return "";
                }
            }
            return null;
        }
    };

    private InputFilter mAmountFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if(dest.length() == 0 && source.equals(".")){
                return "0.";
            }
            String destStr = dest.toString();
            String[] destArr = destStr.split("\\.");
            if (destArr.length > 1) {
                String dotValue = destArr[1];
                if (dotValue.length() == AssetUtil.amountPrecision(mWatchlistData.getCurrentPrice())) {
                    return "";
                }
            }
            return null;
        }
    };

}