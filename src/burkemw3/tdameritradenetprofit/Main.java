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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

    private static class CliOptions {
        static Option OUTPUT_FORMAT = new Option("o", "output-format", true, "text or csv");
    }

    private static enum OutputType {
        TEXT,
        CSV
    }

    public static void main(String[] arguments) {
        Options options = new Options();
        options.addOption(CliOptions.OUTPUT_FORMAT);

        CommandLineParser parser = new GnuParser();
        try {
            CommandLine line = parser.parse(options, arguments);

            String[] remainingArguments = line.getArgs();
            if (remainingArguments.length != 1) {
                throw new ParseException("Expecting path to directory with transactions");
            }

            OutputType outputType = OutputType.TEXT;
            if (true == line.hasOption(CliOptions.OUTPUT_FORMAT.getOpt())) {
                String optionValue = line.getOptionValue(CliOptions.OUTPUT_FORMAT.getOpt());
                if ("text".equals(optionValue)) {
                    outputType = OutputType.TEXT;
                } else if ("csv".equals(optionValue)) {
                    outputType = OutputType.CSV;
                } else {
                    throw new ParseException("output format can only be text or csv");
                }
            }

            List<Transaction> transactions = readTransactionFiles(remainingArguments[0]);
            Collections.sort(transactions);
            Positions positions = processTransactions(transactions);
            updateCurrentPrices(positions);
            output(positions, outputType);
        } catch (ParseException exp) {
            System.err.println("Parsing failed. Reason: " + exp.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String csvOutputFormat = "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n";
    private static final String textOutputFormat = "%6s %10s %10s %10s %10s %10s %8s %10s %10s %9s\n";
    private static final String dollarFormat = "%.2f";
    private static void output(Positions positions, OutputType outputType) {
        int netProfit = 0;
        int dividends = 0;
        int cost = 0;

        if (OutputType.CSV == outputType) {
            System.out.printf(csvOutputFormat,
                    "SYMBOL",
                    "INITIAL OPEN DATE",
                    "INVESTMENT",
                    "DIVIDENDS",
                    "PROCEEDS",
                    "CURRENT VALUE",
                    "COST",
                    "NET PROFIT",
                    "FINAL CLOSE DATE",
                    "ROI");
        } else if (OutputType.TEXT == outputType) {
            System.out.printf(textOutputFormat,
                    "",
                    "INITIAL",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "FINAL",
                    "");
            System.out.printf(textOutputFormat,
                    "",
                    "OPEN",
                    "",
                    "",
                    "",
                    "CURRENT",
                    "",
                    "NET",
                    "CLOSE",
                    "");
            System.out.printf(textOutputFormat,
                    "SYMBOL",
                    "DATE",
                    "INVESTMENT",
                    "DIVIDENDS",
                    "PROCEEDS",
                    "VALUE",
                    "COST",
                    "PROFIT",
                    "DATE",
                    "ROI");
            System.out.printf(textOutputFormat,
                    "------",
                    "----------",
                    "----------",
                    "----------",
                    "----------",
                    "----------",
                    "--------",
                    "----------",
                    "----------",
                    "---------");
        }

        for (Position position : positions.getClosedPositions()) {
            output(outputType, position);

            netProfit += position.getNetProfit();
            dividends += position.getDividends();
            cost += position.getFees();
        }
        for (Position position : positions.getOpenPositions()) {
            output(outputType, position);

            netProfit += position.getNetProfit();
            dividends += position.getDividends();
            cost += position.getFees();
        }

        if (OutputType.TEXT == outputType) {
            System.out.println();
        }

        output(outputType,
                "TOTALS",
                "",
                "",
                String.format(dollarFormat, ((float)dividends)/CURRENCY_FACTOR),
                "",
                "",
                String.format(dollarFormat, ((float)cost)/CURRENCY_FACTOR),
                String.format(dollarFormat, ((float)netProfit)/CURRENCY_FACTOR),
                "",
                ""
                );
    }

    public static void output(OutputType outputType, Position position) {
        DateTime close = position.getFinalClose();
        String closeText = "";
        if (null != close) {
            closeText = close.toString(_mmddyyyyDateFormat);
        }

        output(outputType,
                position.getSymbol(),
                position.getInitialOpen().toString(_mmddyyyyDateFormat),
                String.format(dollarFormat, ((float) position.getInvestment()) / CURRENCY_FACTOR),
                String.format(dollarFormat, ((float) position.getDividends()) / CURRENCY_FACTOR),
                String.format(dollarFormat, ((float) position.getProceeds()) / CURRENCY_FACTOR),
                String.format(dollarFormat, ((float) position.getCurrentValue()) / CURRENCY_FACTOR),
                String.format(dollarFormat, ((float) position.getFees()) / CURRENCY_FACTOR),
                String.format(dollarFormat, ((float) position.getNetProfit()) / CURRENCY_FACTOR),
                closeText,
                String.format("%.2f%%", position.getReturnOnInvestment())
                );
    }

    private static void output(OutputType type, Object... args) {
        switch(type) {
        case CSV:
            System.out.printf(csvOutputFormat, args);
            break;
        case TEXT:
            System.out.printf(textOutputFormat, args);
            break;
        }
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
                    positions.addToPosition(tx.getSymbol(), tx.getDate(), 0, otherTx.getQuantity(), otherTx.getFees());
                    transactions.remove(j);
                    break;
                }
                if (false == foundDrip) {
                    positions.addDividends(tx.getSymbol(), tx.getAmount());
                }
            } else if (true == tx.isBuy()) {
                positions.addToPosition(tx.getSymbol(), tx.getDate(), (-1)*tx.getAmount(), tx.getQuantity(), tx.getFees());
            } else {
                positions.sellPosition(tx.getSymbol(), tx.getDate(), tx.getAmount(), tx.getQuantity(), tx.getFees());
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

                String feesString = csv.get(6);
                long fees = 0;
                if (false == feesString.isEmpty()) {
                    fees = (long) (Float.parseFloat(feesString) * CURRENCY_FACTOR);
                }

                String amountString = csv.get(7);
                Long amount = null;
                if (false == amountString.isEmpty()) {
                    amount = (long) (Float.parseFloat(amountString) * CURRENCY_FACTOR);
                }

                Transaction transaction = new Transaction(date, transactionId, description,
                        quantity, symbol, price, fees, amount);
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
