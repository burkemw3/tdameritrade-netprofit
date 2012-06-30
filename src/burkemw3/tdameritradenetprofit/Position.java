package burkemw3.tdameritradenetprofit;

import org.joda.time.DateTime;

class Position {
    public Position(String symbol, DateTime date, long investment, long quantity, long cost) {
        if (investment <= 0) {
            throw new IllegalArgumentException();
        }
        if (quantity < 0) {
            throw new IllegalArgumentException();
        }

        _symbol = symbol;
        _cost = cost;
        _initialOpen = date;
        _investment = investment;
        _quantity = quantity;
    }

    public void addTo(long investment, long quantity, long cost) {
        if (investment < 0) {
            throw new IllegalArgumentException();
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException();
        }
        if (cost < 0) {
            throw new IllegalArgumentException();
        }
        _investment += investment;
        _quantity += quantity;
        _cost += cost;
    }

    public void addEarnings(long earnings) {
        if (earnings <= 0) {
            throw new IllegalArgumentException();
        }
        _earnings += earnings;
    }

    public void sell(long proceeds, long quantity, DateTime date, long cost) {
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
        if (cost < 0) {
            throw new IllegalArgumentException();
        }
        _quantity = newQuantity;
        _proceeds += proceeds;
        _cost += cost;

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

    public long getEarnings() {
        return _earnings;
    }

    public long getCost() {
        return _cost;
    }

    public long getProceeds() {
        return _proceeds;
    }

    public long getNetProfit() {
        return _currentValue + _earnings + _proceeds - _investment - _cost;
    }

    private String _symbol;
    private DateTime _initialOpen;
    private DateTime _finalClose;
    private long _cost;
    private long _investment;
    private long _quantity;
    private long _earnings;
    private long _proceeds;
    private long _currentValue;
}