package burkemw3.tdameritradenetprofit;

import org.joda.time.DateTime;

class Transaction implements Comparable<Transaction> {
    DateTime _date;
    String _transactionId;
    String _description;
    Long _quantity;
    String _symbol;
    Long _price;
    long _fees;
    long _amount;

    public Transaction(DateTime date, String transactionId, String description, Long quantity,
            String symbol, Long price, long fees, long amount) {
        _date = date;
        _transactionId = transactionId;
        _description = description;
        _quantity = quantity;
        _symbol = symbol;
        _price = price;
        _fees = fees;
        _amount = amount;
    }

    public String getTransactionId() {
        return _transactionId;
    }

    @Override
    public int compareTo(Transaction arg0) {
        return getTransactionId().compareTo(arg0.getTransactionId());
    }

    public boolean isDividend() {
        return _description.contains("DIVIDEND") || _description.contains("GAIN");
    }

    public boolean isBuy() {
        if (true == isDividend()) {
            return false;
        }
        return _amount < 0;
    }

    public String getSymbol() {
        return _symbol;
    }

    public DateTime getDate() {
        return _date;
    }

    public long getAmount() {
        return _amount;
    }

    public Long getQuantity() {
        return _quantity;
    }

    public long getFees() {
        return _fees;
    }
}