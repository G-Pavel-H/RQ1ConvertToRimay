import java.io.*;
import java.util.*;
import java.util.Iterator;

import com.opencsv.CSVWriter;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.extractor.*;
import org.apache.poi.ss.usermodel.DataFormatter;

import org.apache.poi.openxml4j.opc.*;


public class readWordFile {

    //public static String pathWordDocuments = "/Users/alvaro.veizaga/req-pattern-discovery/SRAs/microsoft-word";
    //public static String pathWordDocuments = "/Users/alvaro.veizaga/Documents/PhD/Projects/AutoDiscoveryPatterns/repository/req-pattern-discovery/SRAs/borrar_test2";
    public static String pathWordDocuments = "/Users/alvaro.veizaga/Documents/PhD/Projects/AutoDiscoveryPatterns/repository/req-pattern-discovery/SRAs/microsoft-word";
    //public static String pathWordDocuments = "/Users/alvaro.veizaga/Documents/PhD/Projects/AutoDiscoveryPatterns/ToFix";

    public static Boolean checkIfRequirementTable(XWPFTable tableIn)
    {
        boolean isItRequirement = false;
        for (int i = 0; i < tableIn.getRow(0).getTableCells().size(); i++)
        {
            String cellTable = tableIn.getRow(0).getCell(i).getText();
            if ("Req. ID".equalsIgnoreCase(cellTable))
            {
                isItRequirement = true;
                break;
            }
        }
        return isItRequirement;
    }

    public static Boolean checkIfGlossaryTable(XWPFTable tableIn)
    {
        boolean isItGlossary = false;
        for (int i = 0; i < tableIn.getRow(0).getTableCells().size(); i++)
        {
            String cellTable = tableIn.getRow(0).getCell(i).getText();
            if ("Description".equalsIgnoreCase(cellTable))
            {
                isItGlossary = true;
                break;
            }
        }
        return isItGlossary;
    }


    public static Boolean checkEmptyField(String field)
    {
        boolean isEmpty = false;
        if(field.trim().length() == 0)
        {
            isEmpty = true;
        }
        return  isEmpty;
    }


    public static void extractTitles()
    {
        String sraDocument = "/Users/alvaro.veizaga/Documents/PhD/Drona_ClearStream/Case Studies Documents/NotReviewedYet/MTP_1CASCustodyTransactionManagementPhaseSRA_V0.95tr.docx";
        try
        {
            FileInputStream fis = new FileInputStream(sraDocument);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            Iterator<IBodyElement> bodyElementIterator = xdoc.getBodyElementsIterator();


            while (bodyElementIterator.hasNext()) {
                IBodyElement element = bodyElementIterator.next();
                // All the tables in the first column they have "Req. ID"
                String elementTable = element.getElementType().name();
                System.out.println(elementTable);

                int counter = 0;
                if ("PARAGRAPH".equalsIgnoreCase(element.getElementType().name())) {
                    element.getElementType();

                }
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }


    }

    public static void readRequirementsTable(String sraDocument)
    {
//

//        HashMap<String, String> hmap = new HashMap<String, String>();
//        boolean flagHashMap = false;


        try {
            FileInputStream fis = new FileInputStream(sraDocument);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            Writer fileWriter = new FileWriter(sraDocument+".csv");
            // It needs to send an array per row
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            csvWriter.writeNext(new String[]{"Header","Req ID","Description","Rational","Ubiquitous","Event-driven","Unwanted behavior","State driven","Optional","Complex","Main verb","Intent type","Composed","Missing Information", "Representable on a Class Diagram", "No EARS","Gives information on Domain Model"});

            // Always we have to use <> to indicate the type
            List<XWPFTable> tableList = xdoc.getTables();

            for (XWPFTable table : tableList)
            {
                if (checkIfRequirementTable(table)) {
                    for (int i = 0; i < table.getRows().size(); i++) {

                        if (i > 0)
                        {
                            int numberColumns = table.getRow(i).getTableCells().size();
                            String[] valuesColumn = new String[numberColumns];
                            for (int j = 0; j < ((table.getRow(i).getTableCells().size())); j++) {

                                if(checkEmptyField(table.getRow(i).getCell(j).getText()))
                                {

                                    valuesColumn[j] = table.getRow(i).getCell(j).getText()+"<blank>";

                                }else
                                {
                                    valuesColumn[j] = table.getRow(i).getCell(j).getText();
                                }
                            }

                            if(!hasBlankRow(valuesColumn))
                            {

                                csvWriter.writeNext(valuesColumn);
                            }
//                            flagHashMap = false;
                        }

                    }

                }
            }
            csvWriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    public static void getFuncRequirements(String sraDocument)
    {
        try {
            FileInputStream fis = new FileInputStream(sraDocument);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            XWPFStyles styles = xdoc.getStyles();
            List<XWPFParagraph> xwpfparagraphs =xdoc.getParagraphs();
            boolean isFuncRequirement = false;
            Writer fileWriter = new FileWriter(sraDocument+".csv");
            // It needs to send an array per row
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            csvWriter.writeNext(new String[]{"Header","Req ID","Description","Rationale","Generalized Description","Part A-(Scope (SCOPE - (WHILE| WHEN | WHERE | IF | TEMPORAL))","Part B - Class  (GRAMMAR RULE NAME)"});
            Iterator<IBodyElement> iter = xdoc.getBodyElementsIterator();
            String nameStyle = null;
            int counter = 0;
            while (iter.hasNext())
            {
                counter += 1;

                IBodyElement elem = iter.next();
                if (elem instanceof XWPFParagraph)
                {
                    String style = ((XWPFParagraph) elem).getStyleID();
                    XWPFStyle paragraphStyle =  styles.getStyle(style);

                    if(paragraphStyle != null)
                    {
                        if(fileContainRequirements((XWPFParagraph) elem))
                        {
                            nameStyle = paragraphStyle.getName().toLowerCase().trim(); //heading 1 or 2
                            //System.out.println(nameStyle);
                            isFuncRequirement = true;


                        }else if(getHeadingForRequirements(paragraphStyle).equals(nameStyle))
                        {
                            isFuncRequirement = false;
                        }

                    }

                } else if (elem instanceof XWPFTable && isFuncRequirement)
                {

                    // I
                    if(checkIfRequirementTable((XWPFTable) elem))
                    {
                        for (int i = 0; i < ((XWPFTable) elem).getRows().size(); i++) {

                            if (i > 0)
                            {
                                int numberColumns = ((XWPFTable) elem).getRow(i).getTableCells().size();
                                String[] valuesColumn = new String[numberColumns];
                                for (int j = 0; j < ((((XWPFTable) elem).getRow(i).getTableCells().size())); j++)
                                {
                                    if(checkEmptyField(((XWPFTable) elem).getRow(i).getCell(j).getText()))
                                    {
                                        valuesColumn[j] = ((XWPFTable) elem).getRow(i).getCell(j).getText()+"<blank>";
                                    }else
                                    {

                                        List<XWPFParagraph> cellParagraphs =  ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();


                                        if(cellParagraphs.size()>1)
                                        {
                                            Map singleText = convertToSingleParagraph(cellParagraphs);

                                            valuesColumn[j] = preprocessing((String)singleText.get("singleText"));
                                        }else
                                        {
                                            valuesColumn[j] = preprocessing(((XWPFTable) elem).getRow(i).getCell(j).getText());
                                        }

                                    }
                                }

                                if(!hasBlankRow(valuesColumn))
                                {
                                    csvWriter.writeNext(valuesColumn);
                                }
                            }
                        }
                    }
                }
            }
            csvWriter.close();

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }


    }

    public static Boolean fileContainRequirements(XWPFParagraph paragraphContent)
    {
        boolean containRequirements = false;
        if( paragraphContent.getText().toLowerCase().trim().equals("functional requirements") ||  paragraphContent.getText().toLowerCase().trim().equals("transition requirements"))
        {
            System.out.println(paragraphContent.getText().toLowerCase());
            System.out.println(paragraphContent.getStyle());
            containRequirements = true;
        }
        return containRequirements;
    }

    public static String getHeadingForRequirements(XWPFStyle paragraphStyle)
    {
        String style = paragraphStyle.getName().toLowerCase().trim();
        return style;
    }

    public static Map convertToSingleParagraph(List<XWPFParagraph> paragraphs)
    {
        String singleText = new String();
        List<String> listFormats=new ArrayList<String>();
        for(int i=0; i<paragraphs.size(); i++)
        {
            //
//            System.out.println("AB:"+paragraphs.get(i).getText());

            String paragraphStyle = paragraphs.get(i).getNumFmt();

            if(paragraphStyle != null)
            {
                if(!listFormats.contains(paragraphStyle))
                {
                    listFormats.add(paragraphStyle);
                }
                paragraphStyle = "<"+paragraphStyle.trim()+">";
            }else
            {
                paragraphStyle = "";
            }
            if(paragraphs.get(i).getText().trim().length() > 0)
            {
                if(i!=0) {
                    singleText += "<lb>" + paragraphStyle + paragraphs.get(i).getText().trim() + " ";
                }else
                {
                    singleText += paragraphs.get(i).getText().trim() + " ";
                }
            }
        }
        String formats = "no";
        if(listFormats.size()>0)
        {
            formats = String.join(",", listFormats);
        }

        Map dataToReturn = new HashMap();
//        System.out.println("AE: "+singleText);
//        System.out.println("AF: "+formats);
        dataToReturn.put("singleText", singleText );
        dataToReturn.put("listFormats", formats);
        return dataToReturn;
    }



    public static ArrayList<ArrayList<String>> convertToMultipleLines(List<XWPFParagraph> paragraphs)
    {
        ArrayList<ArrayList<String>> listOfLines = new ArrayList<ArrayList<String>>();

        for(int i=0; i<paragraphs.size(); i++)
        {
            ArrayList<String> singleLine = new ArrayList<String>();
            String paragraphStyle = paragraphs.get(i).getNumFmt();
            String styleLabel = "no";
            if(paragraphStyle != null)
            {
                paragraphStyle = "<"+paragraphStyle.trim()+">";
                styleLabel = paragraphStyle.trim();
            }else
            {
                paragraphStyle = " ";
            }
            singleLine.add(paragraphStyle+" "+paragraphs.get(i).getText().trim()+" ");
            singleLine.add(styleLabel);
            listOfLines.add(singleLine);
        }
        //It will return a list of list. Each list contains the style label and the decription
        return listOfLines;
    }




    /*public static void readCsvFile()
    {
        String pathFile = "/Users/alvaro.veizaga/Documents/PhD/Drona_ClearStream/outTestAlvaroTables.csv";
        String newFile = "/Users/alvaro.veizaga/Documents/PhD/Drona_ClearStream/onlyRequirementsAlvaroTables.csv";
        // newBufferedReader check the compatibility with others java versions
        try {
            Reader reader = Files.newBufferedReader(Paths.get(pathFile));
            CSVReader csvReader = new CSVReader(reader);

            BufferedWriter columnWriter = new BufferedWriter(new FileWriter(newFile));

            String[] nextRecord;
            System.out.println("===========");
            while ((nextRecord = csvReader.readNext()) != null)
            {
//                System.out.println(nextRecord[2]);
                columnWriter.append(nextRecord[2]);
                columnWriter.append("\n");

            }
            columnWriter.close();

        } catch(Exception ex) {
            ex.printStackTrace();
        }


    }*/

    public static Boolean hasBlankRow(String[] rowData)
    {
        boolean flag = false;
        int noReqColumns = rowData.length;
        int counterBlanksColumns = 0;
        for (int i = 0; i<rowData.length; i++)
        {
            if(rowData[i].trim().equalsIgnoreCase("<blank>"))
            {
                counterBlanksColumns ++;
            }
        }
        if(counterBlanksColumns == noReqColumns)
        {
            flag = true;
        }
        return flag;
    }


    public static void importDocument()
    {
        String sraDocument = "/Users/alvaro.veizaga/Documents/PhD/Drona_ClearStream/MM2128_ETF_Issuance_SRA_v0.1.docx";
        try {
            FileInputStream fis = new FileInputStream(sraDocument);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            XWPFWordExtractor extractor = new XWPFWordExtractor(xdoc);
            System.out.println(extractor.getText());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String preprocessing(String originalText)
    {


        String preprocessedText = null;
        //preprocessedText = removePath(originalText);
        //preprocessedText = removePath2(preprocessedText);
        preprocessedText = removeTextParentheses(originalText);
        preprocessedText = removeAdditionalInfo(preprocessedText);
        preprocessedText = removeExamples(preprocessedText);
        preprocessedText = removeNotes(preprocessedText);
        //System.out.println(preprocessedText);
        preprocessedText = removePointsCommas(preprocessedText);
        //System.out.println(preprocessedText);
        preprocessedText = removeDoubleQuotes(preprocessedText);
        //System.out.println(preprocessedText);
        preprocessedText = removeDoubleSpaces(preprocessedText);

        return preprocessedText;

    }

    public static String removePointsCommas(String text)
    {
        String regexPoints = "(\\x22|\\x2C|\\x2E)+";
        String textAfter = text.replaceAll(regexPoints," ");
        return textAfter;
    }

    public static String removeDoubleQuotes(String text)
    {
        String regexPoints = "(“|”)+";
        String textAfter = text.replaceAll(regexPoints," ");
        return textAfter;
    }


    public static String removeDoubleSpaces(String text)
    {
        String regexSpace = "\\x20{2,}";
        String textAfter = text.replaceAll(regexSpace," ");
        return textAfter;
    }

    public static String removePath(String text)
    {
        String regexPath = "\\S*\\x5c\\S*";
        String textAfter = text.replaceAll(regexPath,"file_path");
        return textAfter;

    }

    public static String removePath2(String text)
    {
        String regexPath2 = "\\S*\\x2F\\S*";
        String textAfter = text.replaceAll(regexPath2,"file_path");
       return textAfter;

    }

    public static String removeTextParentheses(String text)
    {
        String regexPlusInfoOne = "(\\([^\\)]+\\))";
        String textAfter = text.replaceAll(regexPlusInfoOne," ");
        return textAfter;

    }

    public static String removeAdditionalInfo(String text)
    {
        String regexInfoThis = "(\\bThis\\b).?((?!\\b(shall|must)\\b).)*?\\.";
        String textAfter = text.replaceAll(regexInfoThis,"");
        return textAfter;
    }

    public static String removeExamples(String text)
    {
        String regexPlusInfoOne = "(\\bTo illustrate\\b)(.*?\\.)";
        String textAfter = text.replaceAll(regexPlusInfoOne,"");
        return textAfter;
    }

    public static String removeNotes(String text)
    {
        String regexPlusInfoOne = "(\\bNotes\\b).*?\\.";
        String textAfter = text.replaceAll(regexPlusInfoOne,"");
        return textAfter;

    }

    public static void getGlossaries(String sraDocument)
    {
        try {
            FileInputStream fis = new FileInputStream(sraDocument);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            XWPFStyles styles = xdoc.getStyles();
            List<XWPFParagraph> xwpfparagraphs =xdoc.getParagraphs();
            boolean hasGlossary = false;
            Writer fileWriter = new FileWriter(sraDocument+"glossary.csv");
            // It needs to send an array per row
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            csvWriter.writeNext(new String[]{"Term","Description"});

            Iterator<IBodyElement> iter = xdoc.getBodyElementsIterator();
            DataFormatter formatter = new DataFormatter();


            List<String[]> listColumns = new ArrayList();


            while (iter.hasNext())
            {
                IBodyElement elem = iter.next();
                if (elem instanceof XWPFParagraph)
                {
                    String style = ((XWPFParagraph) elem).getStyleID();
                    XWPFStyle paragraphStyle =  styles.getStyle(style);

                    if(paragraphStyle != null)
                    {
                        String nameStyle = paragraphStyle.getName();
                        if(nameStyle.toLowerCase().trim().equals("heading 1") && ((XWPFParagraph) elem).getText().toLowerCase().trim().equals("general information"))
                        {
                            hasGlossary = true;
                        } else if (nameStyle.toLowerCase().trim().equals("heading 1"))
                        {
                            hasGlossary = false;
                        }
                    }

                } else if (elem instanceof XWPFTable && hasGlossary)
                {
                    // I
                    if(checkIfGlossaryTable((XWPFTable) elem))
                    {
                        for (int i = 0; i < ((XWPFTable) elem).getRows().size(); i++) {

                            if (i > 0)
                            {
                                int numberColumns = ((XWPFTable) elem).getRow(i).getTableCells().size()+1;
                                String[] valuesColumn = new String[numberColumns];
                                for (int j = 0; j < ((((XWPFTable) elem).getRow(i).getTableCells().size())); j++)
                                {
                                    if(checkEmptyField(((XWPFTable) elem).getRow(i).getCell(j).getText()))
                                    {
                                        valuesColumn[j] = ((XWPFTable) elem).getRow(i).getCell(j).getText()+"<blank>";
                                    }else
                                    {
                                        List<XWPFParagraph> cellParagraphs =  ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();


                                        if(cellParagraphs.size()>1)
                                        {
                                            Map singleText = convertToSingleParagraph(cellParagraphs);
                                            valuesColumn[j] = (String)singleText.get("singleText");

                                        }else
                                        {
                                            valuesColumn[j] = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                        }
                                    }
                                }


                                if(!hasBlankRow(valuesColumn))
                                {

                                    listColumns.add(valuesColumn);
                                    //Object[] objects = list.get(0);
                                    //csvWriter.writeNext(valuesColumn);
                                }
                            }
                        }
                    }

                }
            }
            csvWriter.close();

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    public static void main(String args[])
    {
        File folder = new File(pathWordDocuments);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {

            /*if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                String nameFile = listOfFiles[i].getName();
                getGlossaries(folder+"/"+nameFile);

            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            } */

            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                String nameFile = listOfFiles[i].getName();
                //readRequirementsTable(folder+"/"+nameFile);
                getFuncRequirements(folder+"/"+nameFile);

            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }

//        readCsvFile();
        //extractTitles();
    }

}


