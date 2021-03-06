package com.cybexmobile.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cybex.provider.market.WatchlistData;
import com.cybexmobile.R;
import com.cybex.basemodule.utils.AssetUtil;

import java.math.RoundingMode;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BuySellOrderRecyclerViewAdapter extends RecyclerView.Adapter<BuySellOrderRecyclerViewAdapter.ViewHolder> {

    public static final int TYPE_BUY = 1;
    public static final int TYPE_SELL = 2;

    public static final int SHOW_DEFAULT = 1;
    public static final int SHOW_ONLY_BUY = 2;
    public static final int SHOW_ONLY_SELL = 3;

    public static final int MAX_ITEM_5 = 5;
    public static final int MAX_ITEM_10 = 10;

    private Context mContext;
    private List<List<String>> mOrders;
    private int mType;
    private int mShowBuySell = SHOW_DEFAULT;
    private OnItemClickListener mListener;
    private WatchlistData mWatchlistData;
    private int mPricePrecision = -1;

    public BuySellOrderRecyclerViewAdapter(Context context, WatchlistData watchlistData, int type, List<List<String>> orders) {
        mContext = context;
        mOrders = orders;
        mType = type;
        mWatchlistData = watchlistData;
    }

    public void setShowBuySell(int showBuySell) {
        mShowBuySell = showBuySell;
        notifyDataSetChanged();
    }

    public void setWatchlistData(WatchlistData watchlistData) {
        mWatchlistData = watchlistData;
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }

    public void setPricePrecision(int precision) {
        mPricePrecision = precision;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buy_sell_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        /**
         * fix bug:CYM-439
         * 委单倒叙排序，从底部开始排
         */
        switch (mType){
            case TYPE_BUY:
                if(mOrders.size() > position){
                    final List<String> order = mOrders.get(position);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(mListener != null){
                                mListener.onItemClick(holder.mOrderPrice.getText().toString());
                            }
                        }
                    });
                    holder.mOrderPrice.setText(AssetUtil.formatNumberRounding(Double.parseDouble(order.get(0)), mPricePrecision == -1 ? mWatchlistData.getPricePrecision() : mPricePrecision));
                    holder.mOrderVolume.setText(AssetUtil.formatAmountToKMB(Double.parseDouble(order.get(1)), mWatchlistData.getAmountPrecision()));
                    float percentage = (float) getPercentage(mOrders, position);
                    LinearLayout.LayoutParams layoutParams_colorBar = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1 - percentage);
                    LinearLayout.LayoutParams layoutParams_colorBarNon = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, percentage);
                    holder.mColorBar.setLayoutParams(layoutParams_colorBar);
                    holder.mColorBarNon.setLayoutParams(layoutParams_colorBarNon);
                    holder.mColorBarNon.setBackgroundColor(Color.TRANSPARENT);
                    holder.mColorBar.setBackgroundColor(mContext.getResources().getColor(R.color.fade_background_green));
                } else {
                    holder.itemView.setOnClickListener(null);
                    holder.mOrderPrice.setText(mContext.getResources().getString(R.string.text_empty));
                    holder.mOrderVolume.setText(mContext.getResources().getString(R.string.text_empty));
                    holder.mColorBarNon.setBackgroundColor(Color.TRANSPARENT);
                    holder.mColorBar.setBackgroundColor(Color.TRANSPARENT);
                }
                break;
            case TYPE_SELL:
                if(mOrders.size() < getItemCount() - position){
                    holder.itemView.setOnClickListener(null);
                    holder.mOrderPrice.setText(mContext.getResources().getString(R.string.text_empty));
                    holder.mOrderVolume.setText(mContext.getResources().getString(R.string.text_empty));
                    holder.mColorBarNon.setBackgroundColor(Color.TRANSPARENT);
                    holder.mColorBar.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    final List<String> order = mOrders.get(getItemCount() - position - 1);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(mListener != null){
                                mListener.onItemClick(holder.mOrderPrice.getText().toString());
                            }
                        }
                    });
                    holder.mOrderPrice.setText(AssetUtil.formatNumberRounding(Double.parseDouble(order.get(0)), mPricePrecision == -1 ? mWatchlistData.getPricePrecision() : mPricePrecision, RoundingMode.UP));
                    holder.mOrderVolume.setText(AssetUtil.formatAmountToKMB(Double.parseDouble(order.get(1)), mWatchlistData.getAmountPrecision()));
                    float percentage = (float) getPercentage(mOrders, mOrders.size() - getItemCount() + position);
                    LinearLayout.LayoutParams layoutParams_colorBar = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1 - percentage);
                    LinearLayout.LayoutParams layoutParams_colorBarNon = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, percentage);
                    holder.mColorBar.setLayoutParams(layoutParams_colorBar);
                    holder.mColorBarNon.setLayoutParams(layoutParams_colorBarNon);
                    holder.mColorBarNon.setBackgroundColor(Color.TRANSPARENT);
                    holder.mColorBar.setBackgroundColor(mContext.getResources().getColor(R.color.fade_background_red));
                }
                break;
        }

        holder.mOrderPrice.setTextColor(mContext.getResources().getColor(mType == TYPE_BUY ? R.color.increasing_color : R.color.decreasing_color));
    }

    @Override
    public int getItemCount() {
        return mShowBuySell == SHOW_DEFAULT ? MAX_ITEM_5 : MAX_ITEM_10;
    }

    @Override
    public int getItemViewType(int position) {
        return mType;
    }

    private double getPercentage(List<List<String>> orderList, int position) {
        double divider = 0;
        double total = getSum(orderList);
        for (int i = 0; i <= position; i++) {
            if(mType == TYPE_BUY){
                divider += Double.parseDouble(orderList.get(i).get(1));
            } else {
                if(i > 0){
                    divider += Double.parseDouble(orderList.get(i - 1).get(1));
                }
            }
        }
        return mType == TYPE_BUY ? divider / total : (total - divider)/total;
    }

    private double getSum(List<List<String>> orderList) {
        double sum = 0;
        if (orderList != null && orderList.size() != 0) {
            for (int i = 0; i < orderList.size(); i++) {
                sum += Double.parseDouble(orderList.get(i).get(1));
            }
        }
        return sum;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_order_price) TextView mOrderPrice;
        @BindView(R.id.tv_order_amount) TextView mOrderVolume;
        @BindView(R.id.tv_color_bar_non) TextView mColorBarNon;
        @BindView(R.id.tv_color_bar) TextView mColorBar;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public interface OnItemClickListener{
        void onItemClick(String price);
    }
}
