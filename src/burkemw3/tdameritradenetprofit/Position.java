package burkemw3.tdameritradenetprofit;

import org.joda.time.DateTime;

class Position {
    public Position(String symbol, DateTime date, long investment, long quantity, long fees) {
        if (investment <= 0) {
            throw new IllegalArgumentException();
        }
        if (quantity < 0) {
            throw new IllegalArgumentException();
        }

        _symbol = symbol;
        _fees = fees;
        _initialOpen = date;
        _investment = investment;
        _quantity = quantity;
    }

    public void addTo(long investment, long quantity, long fees) {
        if (investment < 0) {
            throw new IllegalArgumentException();
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException();
        }
        if (fees < 0) {
            throw new IllegalArgumentException();
        }
        _investment += investment;
        _quantity += quantity;
        _fees += fees;
    }

    public void addDividends(long dividends) {
        if (dividends <= 0) {
            throw new IllegalArgumentException();
        }
        _dividends += dividends;
    }

    public void sell(long proceeds, long quantity, DateTime date, long fees) {
        if (proceeds <= 0) {
            throw new IllegalArgumentException();
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException();
        }
        long newQuantity = _quantity - quantity;
        if (newQuantity < 0) {
            throw new IllegalStateException();
        }
        if (fees < 0) {
            throw new IllegalArgumentException();
        }
        _quantity = newQuantity;
        _proceeds += proceeds;
        _fees += fees;

        if (_quantity == 0) {
            _finalClose = date;
        }
    }

    public boolean isOpen() {
        return _quantity != 0;
    }

    public void updateCurrentPrice(long price) {
        _currentValue = _quantity * price / Main.CURRENCY_FACTOR / (Main.QUANTITY_FACTOR / Main.CURRENCY_FACTOR);
    }

    public String getSymbol() {
        return _symbol;
    }

    public DateTime getInitialOpen() {
        return _initialOpen;
    }

    public DateTime getFinalClose() {
        return _finalClose;
    }

    public long getInvestment() {
        return _investment;
    }

    public long getCurrentValue() {
        return _currentValue;
    }

    public long getDividends() {
        return _dividends;
    }

    public long getFees() {
        return _fees;
    }

    public long getProceeds() {
        return _proceeds;
    }

    public long getNetProfit() {
        return _currentValue + _dividends + _proceeds - _investment - _fees;
    }

    public float getReturnOnInvestment() {
        long gain = _currentValue + _dividends + _proceeds;
        long cost = _investment + _fees;
        return ((float)gain - (float)cost) / (float)cost * 100;
    }

    private String _symbol;
    private DateTime _initialOpen;
    private DateTime _finalClose;
    private long _fees;
    private long _investment;
    private long _quantity;
    private long _dividends;
    private long _proceeds;
    private long _currentValue;
}