package burkemw3.tdameritradenetprofit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.DateTime;

class Positions {
    private Map<String, Position> _openPositions = new TreeMap<String, Position>();
    private List<Position> _closedPositions = new ArrayList<Position>();

    public void addToPosition(String symbol, DateTime date, long investment, Long quantity, long fees) {
        Position position = _openPositions.get(symbol);
        if (position == null) {
            position = new Position(symbol, date, investment, quantity, fees);
            _openPositions.put(symbol, position);
        } else {
            position.addTo(investment, quantity, fees);
        }
    }

    public void addDividends(String symbol, long dividends) {
        Position position = _openPositions.get(symbol);
        if (null == position) {
            for (Position p : _closedPositions) {
                if (true == p.getSymbol().equals(symbol)) {
                    position = p;
                }
            }
        }
        position.addDividends(dividends);
    }

    public void sellPosition(String symbol, DateTime date, long proceeds, long quantity, long fees) {
        Position position = _openPositions.get(symbol);
        if (position == null) {
            throw new IllegalStateException("closing an unopened position for: " + symbol);
        }
        position.sell(proceeds, quantity, date, fees);
        if (false == position.isOpen()) {
            _closedPositions.add(position);
            _openPositions.remove(symbol);
        }
    }

    public void updateCurrentPrice(String symbol, long price) {
        _openPositions.get(symbol).updateCurrentPrice(price);
    }

    public Set<String> getOpenSymbols() {
        return Collections.unmodifiableSet(_openPositions.keySet());
    }

    public Collection<Position> getClosedPositions() {
        return Collections.unmodifiableCollection(_closedPositions);
    }

    public Collection<Position> getOpenPositions() {
        return Collections.unmodifiableCollection(_openPositions.values());
    }
}