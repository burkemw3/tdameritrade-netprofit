package burkemw3.tdameritradenetprofit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import com.csvreader.CsvReader;

public class Main {
    public static final int CURRENCY_FACTOR = 100;
    public static final int QUANTITY_FACTOR = 1000;

    private static final DateTimeFormatter _mmddyyyyDateFormat =
            new DateTimeFormatterBuilder()
            .appendMonthOfYear(2)
            .appendLiteral("/")
            .appendDayOfMonth(2)
            .appendLiteral("/")
            .appendYear(4, 4)
            .toFormatter();

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                throw new IllegalArgumentException("I expect 1 argument, the path to a folder with transactions");
            }

            List<Transaction> transactions = readTransactionFiles(args[0]);
            Collections.sort(transactions);
            Positions positions = processTransactions(transactions);
            updateCurrentPrices(positions);
            output(positions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void output(Positions positions) {
        int netProfit = 0;
        int earnings = 0;
        int cost = 0;
        System.out.println("symbol, initial open date, investment, earnings, proceeds, current value, cost, net profit, final close date");
        for (Position position : positions.getClosedPositions()) {
            System.out.print(position.getSymbol() + ",");
            System.out.print(position.getInitialOpen().toString(_mmddyyyyDateFormat) + ",");
            System.out.print(((float)position.getInvestment())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getEarnings())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getProceeds())/CURRENCY_FACTOR + ",");
            System.out.print("0,");
            System.out.print(((float)position.getCost())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getNetProfit())/CURRENCY_FACTOR + ",");
            System.out.print(position.getFinalClose().toString(_mmddyyyyDateFormat));
            System.out.println();
            netProfit += position.getNetProfit();
            earnings += position.getEarnings();
            cost += position.getCost();
        }
        for (Position position : positions.getOpenPositions()) {
            System.out.print(position.getSymbol() + ",");
            System.out.print(position.getInitialOpen().toString(_mmddyyyyDateFormat) + ",");
            System.out.print(((float)position.getInvestment())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getEarnings())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getProceeds())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getCurrentValue())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getCost())/CURRENCY_FACTOR + ",");
            System.out.print(((float)position.getNetProfit())/CURRENCY_FACTOR + ",");
            System.out.println();
            netProfit += position.getNetProfit();
            earnings += position.getEarnings();
            cost += position.getCost();
        }

        System.out.print("TOTALS,");
        System.out.print(",");
        System.out.print(",");
        System.out.print(((float)earnings)/CURRENCY_FACTOR + ",");
        System.out.print(",");
        System.out.print(",");
        System.out.print(((float)cost)/CURRENCY_FACTOR + ",");
        System.out.print(((float)netProfit)/CURRENCY_FACTOR + ",");
        System.out.println();
    }

    private static Positions processTransactions(List<Transaction> transactions) {
        Positions positions = new Positions();
        for (int i=0 ; i<transactions.size() ; ++i) {
            Transaction tx = transactions.get(i);
            if (true == tx.isDividend()) {
                boolean foundDrip = false;
                for (int j=i+1 ; j<transactions.size() ; ++j) {
                    Transaction otherTx = transactions.get(j);
                    if (false == otherTx.getDate().equals(tx.getDate())) {
                        break;
                    }
                    if (false == otherTx.isBuy()) {
                        continue;
                    }
                    if (false == tx.getSymbol().equals(otherTx.getSymbol())) {
                        continue;
                    }
                    if (tx.getAmount() != (-1)*otherTx.getAmount()) {
                        continue;
                    }
                    foundDrip = true;
                    positions.addToPosition(tx.getSymbol(), tx.getDate(), 0, otherTx.getQuantity(), otherTx.getCost());
                    transactions.remove(j);
                    break;
                }
                if (false == foundDrip) {
                    positions.addEarnings(tx.getSymbol(), tx.getAmount());
                }
            } else if (true == tx.isBuy()) {
                positions.addToPosition(tx.getSymbol(), tx.getDate(), (-1)*tx.getAmount(), tx.getQuantity(), tx.getCost());
            } else {
                positions.sellPosition(tx.getSymbol(), tx.getDate(), tx.getAmount(), tx.getQuantity(), tx.getCost());
            }
        }
        return positions;
    }

    private static List<Transaction> readTransactionFiles(String directoryPath) throws IOException {
        List<Transaction> transactions = new ArrayList<Transaction>();
        File directory = new File(directoryPath);
        for (File file : directory.listFiles()) {
            if (false == file.getPath().endsWith("csv")) {
                continue;
            }

            CsvReader csv = new CsvReader(file.getAbsolutePath());
            csv.skipRecord();
            while (csv.readRecord()) {
                if (true == csv.getRawRecord().equals("***END OF FILE***")) {
                    break;
                }

                String symbol = csv.get(4);
                if (symbol.isEmpty() || symbol.equals("MMDA10") || symbol.equals("MMDA1")) {
                    continue;
                }

                DateTime date = DateTime.parse(csv.get(0), _mmddyyyyDateFormat);

                String transactionId = csv.get(1);

                String description = csv.get(2);

                String quantityString = csv.get(3);
                Long quantity = null;
                if (false == quantityString.isEmpty()) {
                    quantity = (long) (Float.parseFloat(quantityString) * QUANTITY_FACTOR);
                }

                String priceString = csv.get(5);
                Long price = null;
                if (false == priceString.isEmpty()) {
                    price = (long) (Float.parseFloat(priceString) * CURRENCY_FACTOR);
                }

                String costString = csv.get(6);
                long cost = 0;
                if (false == costString.isEmpty()) {
                    cost = (long) (Float.parseFloat(costString) * CURRENCY_FACTOR);
                }

                String amountString = csv.get(7);
                Long amount = null;
                if (false == amountString.isEmpty()) {
                    amount = (long) (Float.parseFloat(amountString) * CURRENCY_FACTOR);
                }

                Transaction transaction = new Transaction(date, transactionId, description,
                        quantity, symbol, price, cost, amount);
                transactions.add(transaction);
            }
        }

        return transactions;
    }

    public static void updateCurrentPrices(Positions positions)
            throws InterruptedException, IOException {
        String urlSymbols = join(positions.getOpenSymbols(), "+");
        String urlString = String.format(
            "http://finance.yahoo.com/d/quotes.csv?s=%s&f=sl1", urlSymbols);

        CsvReader csv;
        int i = 0;
        while(true) {
            try {
                URL theUrl = new URL(urlString);
                InputStream is = theUrl.openStream();
                csv = new CsvReader(is, Charset.defaultCharset());
                break;
            } catch (IOException e) {
                if (i >= 3) {
                    throw e;
                } else {
                    Thread.sleep(1000 * 30 /* 30 seconds */);
                }
            }
            ++i;
        }

        while(csv.readRecord()) {
            String symbol = csv.get(0);
            long close = (long)(Float.parseFloat(csv.get(1))*CURRENCY_FACTOR);
            positions.updateCurrentPrice(symbol, close);
        }
    }

    private static String join(Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
              break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }
}
