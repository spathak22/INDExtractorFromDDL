package ddlind.main;

import ddlind.sql.DDLParser;
import ddlind.sql.TableObject;

import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ddlind.util.FileUtil;
import ddlind.util.Constants;

public class DDLIndMain {

    @Option(name = "-ddl", usage = "Input file location", required = true)
    private String ddl;

    @Option(name = "-outputPath", usage = "Output file location", required = true)
    private String outputPath;

    final static Logger logger = Logger.getLogger(DDLIndMain.class);

    public static void main(String[] args) {
        DDLIndMain ddlIndMain = new DDLIndMain();
        ddlIndMain.extractIndFromDDL(args);
    }

    public void extractIndFromDDL(String[] args) {
        // Parse the arguments
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            logger.error(e.getMessage());
            parser.printUsage(System.out);
            return;
        }

        logger.info("Processing input");
        Map<String, TableObject> tables = this.readSQLFile(ddl);
        List<String> inds = new ArrayList<String>();
        List<String> invalidPks = new ArrayList<String>();
        Map<String, Integer> ordinalPos = new HashMap<String, Integer>();
        this.generateInds(tables, inds, invalidPks, ordinalPos);
        logger.info("Processing input");
        FileUtil.writeIndsToJsonFormat(inds,ordinalPos,outputPath+Constants.FileName.IND_SCHEMA_FILE.getValue());
        if(!invalidPks.isEmpty()) {
            logger.info("Invalid primary keys found");
            FileUtil.writeInvalidPksToFile(invalidPks, outputPath+Constants.FileName.INVALID_PKS_FILE.getValue());
        }
    }

    public String extractIndFromDDL(String ddl, String outputPath) {
        logger.info("Processing input");
        Map<String, TableObject> tables = this.readSQLFile(ddl);
        List<String> inds = new ArrayList<String>();
        List<String> invalidPks = new ArrayList<String>();
        Map<String, Integer> ordinalPos = new HashMap<String, Integer>();
        this.generateInds(tables, inds, invalidPks, ordinalPos);
        logger.info("Processing input");
        String result = FileUtil.writeIndsToJsonFormat(inds,ordinalPos,outputPath+Constants.FileName.IND_SCHEMA_FILE.getValue());
        if(!invalidPks.isEmpty()) {
            logger.info("Invalid primary keys found");
            FileUtil.writeInvalidPksToFile(invalidPks, outputPath+Constants.FileName.INVALID_PKS_FILE.getValue());
        }
        if(result.equals("Success"))
            return outputPath+Constants.FileName.IND_SCHEMA_FILE.getValue();
        return "Failed";
    }


    public Map<String, TableObject> readSQLFile(String inputPath) {
        DDLParser ddlParser = new DDLParser();
        return ddlParser.parseDDLFile(inputPath);
    }

    public void generateInds(Map<String, TableObject> tables, List<String> inds, List<String> invalidPks, Map<String, Integer> ordinalPos) {
        for (Map.Entry<String, TableObject> tableEntry : tables.entrySet()) {
            TableObject table = tableEntry.getValue();
            List<ForeignKeyIndex> fkList = table.getForeignKey();
            for (ForeignKeyIndex index : fkList) {
                isForeignKeyValid(index, tables, table, inds, invalidPks, ordinalPos);
            }
        }
    }


    //Create primary key map , table and pk column
    public void isForeignKeyValid(ForeignKeyIndex index, Map<String, TableObject> tables, TableObject baseTable, List<String> inds, List<String> invalidPks, Map<String, Integer> ordinalPos) {
        String refTableName = index.getTable().getName();
        String baseTableName = baseTable.getName();
        List<String> refColName = index.getReferencedColumnNames();
        List<String> baseColumnName = index.getColumnsNames();

        TableObject refTable = tables.get(refTableName);
        int count = 0;
        for (String refCol : refColName) {
            if (refTable.getPrimaryKeyCols().contains(refCol)) {
                inds.add(Constants.Regex.OPEN_PARENTHESIS.getValue() + baseTableName + Constants.Regex.PERIOD.getValue() + baseColumnName.get(count) + Constants.Regex.PARENTHESIS.getValue() + refTableName + Constants.Regex.PERIOD.getValue() + refCol + Constants.Regex.CLOSE_PARENTHESIS.getValue() + "\n");
                setOrdinalPosition(baseTable, baseColumnName.get(count), refTable, refCol, ordinalPos);
                count++;
            } else {
                invalidPks.add(refTableName + Constants.Regex.PERIOD.getValue() + refCol);
            }
        }
    }

    public void setOrdinalPosition(TableObject baseTable, String baseColumn, TableObject refTable, String refCol, Map<String, Integer> ordinalPos) {
        Map<String,Integer> baseTableOrdinalPos = baseTable.getOrdinalPosition();
        Map<String,Integer> refTableOrdinalPos = refTable.getOrdinalPosition();
        ordinalPos.put(baseTable.getName() + Constants.Regex.PERIOD.getValue() + baseColumn, baseTableOrdinalPos.get(baseColumn));
        ordinalPos.put(refTable.getName() + Constants.Regex.PERIOD.getValue() + refCol, refTableOrdinalPos.get(refCol));
    }
}
