package com.cybex.provider.graphene.chain;

//交易对
public class AssetsPair {

    private String base;
    private String quote;
    private int order;
    private AssetObject baseAsset;
    private AssetObject quoteAsset;
    private Config config;
    private Form form;

    public AssetsPair(String base, String quote) {
        this.base = base;
        this.quote = quote;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public AssetObject getBaseAsset() {
        return baseAsset;
    }

    public void setBaseAsset(AssetObject baseAsset) {
        this.baseAsset = baseAsset;
    }

    public AssetObject getQuoteAsset() {
        return quoteAsset;
    }

    public void setQuoteAsset(AssetObject quoteAsset) {
        this.quoteAsset = quoteAsset;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public Form getForm() {
        return form;
    }

    public class Config {
        public String last_price;
        public String amount;
        public String total;
    }

    public class Form {
        public String min_trade_amount;
        public String amount_step;
        public String price_step;
        public String min_order_value;
        public String total_step;
    }
}
