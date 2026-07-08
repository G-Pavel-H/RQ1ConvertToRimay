import java.io.*;
import java.util.*;
import java.util.Iterator;
import com.opencsv.CSVWriter;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.openxml4j.opc.*;


public class readWordFileSoftPreprocessing {

    public static String pathWordDocuments = "path to folder";
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


    public static Boolean fileContainRequirements(XWPFParagraph paragraphContent)
    {
        boolean containRequirements = false;

        if( paragraphContent.getText().toLowerCase().trim().equals("functional requirements") ||
                paragraphContent.getText().toLowerCase().trim().equals("transition requirements") ||
                paragraphContent.getText().toLowerCase().trim().equals("transition/cutover requirements") ||
                paragraphContent.getText().toLowerCase().trim().equals("non-functional requirements"))
        {
            System.out.println(paragraphContent.getText().toLowerCase().trim());
            containRequirements = true;

        }
        return containRequirements;
    }

    //public static Boolean hasBlankRow(String[] rowData)
    public static Boolean hasBlankRow(ArrayList<String> rowData)
    {
        boolean flag = false;
        int noReqColumns = rowData.size();

//        System.out.println("Z: "+noReqColumns);

        int counterBlanksColumns = 0;
        for(String el : rowData)
        {
//            System.out.println("A1");
            if(el.trim().equalsIgnoreCase("<blank>"))
            {
//                System.out.println("A2");
                counterBlanksColumns ++;
            }
        }
        if(counterBlanksColumns == noReqColumns)
        {
//            System.out.println("A3");
            flag = true;
        }
        return flag;
    }




    public static void createCsv(List<List<String>> requirements, String sraDocument)
    {
        try {
            Writer fileWriter = new FileWriter(sraDocument+".csv", false);
            // It needs to send an array per row
            CSVWriter csvWriter = new CSVWriter(fileWriter,';',CSVWriter.DEFAULT_QUOTE_CHARACTER,CSVWriter.DEFAULT_ESCAPE_CHARACTER,CSVWriter.DEFAULT_LINE_END);
            //Production csvWriter.writeNext(new String[]{"Header","Req ID","Description","Regex","Before","Rationale","Generalized Description","Part A-(Scope (SCOPE - (WHILE| WHEN | WHERE | IF | TEMPORAL))","Part B - Class  (GRAMMAR RULE NAME)"});
            csvWriter.writeNext(new String[]{"Header","Req ID","Descr. After Regex","Regex"," Descr. Before Regex","List","# Words","Rationale"});

            for(List<String> reqInDoc : requirements)
            {
                csvWriter.writeNext(reqInDoc.toArray(new String[0]));
            }
            csvWriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }


    public static List<List<String>> getFuncRequirements(String sraDocument)
    {
        List<List<String>> requirements = new ArrayList<List<String>>();
        try {
            FileInputStream fis = new FileInputStream(sraDocument);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            XWPFStyles styles = xdoc.getStyles();
            List<XWPFParagraph> xwpfparagraphs =xdoc.getParagraphs();

            boolean isFuncRequirement = false;
            Iterator<IBodyElement> iter = xdoc.getBodyElementsIterator();
            String nameStyle = null;


            int counter = 0;
            while (iter.hasNext())
            {
                counter += 1;

                IBodyElement elem = iter.next();
                //Capture the heading
                isFuncRequirement = isSectionFuncReq(elem, styles, isFuncRequirement );
                if (elem instanceof XWPFTable && isFuncRequirement)
                {
                    if(checkIfRequirementTable((XWPFTable) elem))
                    {
                        XWPFTableRow header = ((XWPFTable)elem).getRow(0);
                        for (int i = 0; i < ((XWPFTable) elem).getRows().size(); i++) {


                            if (i > 0)
                            {
                                int numberColumns = ((XWPFTable) elem).getRow(i).getTableCells().size();
                                ArrayList<String> valuesColumn = new ArrayList<String>();
                                //String[] valuesColumn = new String[numberColumns];
                                //System.out.println("SIZE"+((((XWPFTable) elem).getRow(i).getTableCells().size())));
                                for (int j = 0; j < ((((XWPFTable) elem).getRow(i).getTableCells().size())); j++)
                                {
                                    if(!checkEmptyField(((XWPFTable) elem).getRow(i).getCell(j).getText()))
                                    {
//                                        System.out.println("2");
                                        List<XWPFParagraph> cellParagraphs =  ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();

//                                        System.out.println("header: "+ header.getCell(j).getText() );

                                        if(cellParagraphs.size()>1)
                                        {
//                                            System.out.println("3");
//                                            System.out.println("header: "+header.getCell(j).getText().trim().toLowerCase());
                                            if((header.getCell(j).getText().trim().toLowerCase()).equals("description"))
                                            {
                                                Map singleText = readWordFile.convertToSingleParagraph(cellParagraphs);
                                                Map preprocessData = PreprocessingReq.preprocessing((String)singleText.get("singleText"));
//                                                System.out.println("C: "+(String)singleText.get("singleText"));
                                                valuesColumn.add((String)preprocessData.get("text")); //preprocessed
                                                valuesColumn.add(String.valueOf(preprocessData.get("boolean")));
                                                valuesColumn.add((String)singleText.get("singleText"));
                                                valuesColumn.add((String)singleText.get("listFormats"));
                                                int wordsReq = countWords((String)preprocessData.get("text"));
                                                valuesColumn.add(String.valueOf(wordsReq));

                                            }else //other columns
                                            {
                                                Map singleText = readWordFile.convertToSingleParagraph(cellParagraphs);
//                                                System.out.println("D"+j+" : "+(String)singleText.get("singleText"));
                                                valuesColumn.add((String)singleText.get("singleText"));
                                            }
                                        }else //no paragraphs
                                        {
//                                            System.out.println("4");
                                            if((header.getCell(j).getText().trim().toLowerCase()).equals("description"))
                                            {
                                                Map preprocessData = PreprocessingReq.preprocessing(((XWPFTable) elem).getRow(i).getCell(j).getText());
                                                valuesColumn.add((String)preprocessData.get("text") );    //preprocessed
                                                valuesColumn.add(String.valueOf(preprocessData.get("boolean")));
                                                valuesColumn.add(((XWPFTable) elem).getRow(i).getCell(j).getText());
                                                valuesColumn.add("no"); //no list formats
                                                int wordsReq = countWords((String)preprocessData.get("text"));
                                                valuesColumn.add(String.valueOf(wordsReq));
                                            }else
                                            {
                                                valuesColumn.add(((XWPFTable) elem).getRow(i).getCell(j).getText());

                                            }
                                        }
                                    }else //to fill out the blank column rows
                                    {
                                        valuesColumn.add("<blank>");
                                    }
                                }
//                                System.out.println("Alvaro");
//                                System.out.println(valuesColumn.size());
//                                for(int k=0; k<= valuesColumn.size(); k++)
//                                {
//                                    System.out.println(valuesColumn.get(k));
//                                }

                                if(!hasBlankRow(valuesColumn))
                                {
                                    requirements.add(valuesColumn);
                                }
                            }
                        }
                    }
                }
            }

            //csvWriter.close();


        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return requirements;
    }


    public static Boolean hasMultipleParagraphs(XWPFTable elem, int row, int cell)
    {
        Boolean multipleParagraphsDescription = false;
        List<XWPFParagraph> cellParagraphs = elem.getRow(row).getCell(cell).getParagraphs();
        System.out.println("Size is: "+cellParagraphs.size());
        if (cellParagraphs.size() > 1) {
            multipleParagraphsDescription = true;
        }
        return multipleParagraphsDescription;
    }


    public static List<List<String>> getFuncReqPerLine(String sraDocument)
    {
        List<List<String>> requirements = new ArrayList<List<String>>();
        try {
            FileInputStream fis = new FileInputStream(sraDocument);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            XWPFStyles styles = xdoc.getStyles();
            List<XWPFParagraph> xwpfparagraphs =xdoc.getParagraphs();
            boolean isFuncRequirement = false;
            Iterator<IBodyElement> iter = xdoc.getBodyElementsIterator();
            String nameStyle = null;


            int counter = 0;
            while (iter.hasNext())
            {
                counter += 1;

                IBodyElement elem = iter.next();
                isFuncRequirement = isSectionFuncReq(elem, styles, isFuncRequirement);

                if (elem instanceof XWPFTable && isFuncRequirement)
                {
                    if(checkIfRequirementTable((XWPFTable) elem))
                    {
                        XWPFTableRow header = ((XWPFTable)elem).getRow(0);//We assume all tables have headers
                        for (int i = 0; i < ((XWPFTable) elem).getRows().size(); i++) {


                            if (i > 0) //not to read the header
                            {
                                int numberColumns = ((XWPFTable) elem).getRow(i).getTableCells().size();
                                ArrayList<String> valuesColumn = new ArrayList<String>();
                                //String[] valuesColumn = new String[numberColumns];
                                //System.out.println("SIZE"+((((XWPFTable) elem).getRow(i).getTableCells().size())));

                                String headerTable = "<blank>";
                                String reqIdTable = "<blank>";
                                String descTable = "<blank>";
                                String rationalTable = "<blank>";
                                Boolean multipleParagraphsDescription = false;
                                Boolean multipleParagraphsRational = false;
                                ArrayList<ArrayList<String>> multipleLines = new ArrayList<ArrayList<String>>();

                                for (int j = 0; j < ((((XWPFTable) elem).getRow(i).getTableCells().size())); j++)
                                {
                                    //gathering all the elements from the requirements table
                                    if (!checkEmptyField(((XWPFTable) elem).getRow(i).getCell(j).getText())) {

                                        switch (header.getCell(j).getText().trim().toLowerCase()){

                                            case "header":
//                                                System.out.println("A");
                                                headerTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                                break;
                                            case "req. id":
//                                                System.out.println("B");
                                                reqIdTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                                System.out.println("ReqID"+reqIdTable);
                                                break;
                                            case "description":
//                                                System.out.println("C");
                                                multipleParagraphsDescription = hasMultipleParagraphs((XWPFTable) elem,i,j);
                                                if(multipleParagraphsDescription)
                                                {
                                                    List<XWPFParagraph> cellParagraphs = ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();
                                                    multipleLines = readWordFile.convertToMultipleLines(cellParagraphs);
                                                    descTable = "multiple";
                                                } else {
                                                    descTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                                }
                                                System.out.println("Req: "+descTable);
                                                break;
                                            case "rational":
//                                                System.out.println("D");
                                                multipleParagraphsRational = hasMultipleParagraphs((XWPFTable) elem,i,j);
                                                if(multipleParagraphsRational){
                                                    List<XWPFParagraph> cellParagraphs = ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();
                                                    Map singleText = readWordFile.convertToSingleParagraph(cellParagraphs);
                                                    rationalTable = (String) singleText.get("singleText");
                                                }else{
                                                    rationalTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                                }
                                                break;
                                        }
                                    }
                                }

                                if(! multipleParagraphsDescription){ //if it is a requirement with one paragraph in the desc
//                                    System.out.println("A");
                                    Map preprocessData = PreprocessingReq.preprocessing(descTable);
                                    String descrReq = (String)preprocessData.get("text");
                                    String[] listPhrasesInReq = descrReq.split("\n");
                                    if(listPhrasesInReq.length>1) //If within the requirement it has multiple lines
                                    {
//                                        System.out.println("B");
                                        for(int m=0; m<listPhrasesInReq.length; m++)
                                        {
                                            ArrayList<String> singleLine = new ArrayList<String>();
                                            singleLine.add(headerTable);
                                            singleLine.add(reqIdTable+"-A"+(m+1));
                                            singleLine.add(listPhrasesInReq[m]); //split text
                                            singleLine.add(String.valueOf(preprocessData.get("boolean"))); // if it has been preprocessed
                                            singleLine.add(descTable);
                                            singleLine.add("no");//format
                                            int wordsReq = countWords(listPhrasesInReq[m]);
                                            singleLine.add(String.valueOf(wordsReq));
                                            singleLine.add(rationalTable);
                                            //adding multiple requirements
                                            if(!hasBlankRow(singleLine))
                                            {
                                                requirements.add(singleLine);
                                            }
                                        }
                                    }else
                                    {
//                                        System.out.println("C");
                                        valuesColumn.add(headerTable);
                                        //System.out.println("ReqID 2: "+reqIdTable);
                                        valuesColumn.add(reqIdTable);
                                        valuesColumn.add((String)preprocessData.get("text") );    //preprocessed
                                        valuesColumn.add(String.valueOf(preprocessData.get("boolean")));
                                        valuesColumn.add(descTable);
                                        valuesColumn.add("no"); //no list formats
                                        int wordsReq = countWords((String)preprocessData.get("text"));
                                        valuesColumn.add(String.valueOf(wordsReq));
                                        valuesColumn.add(rationalTable);

                                        if(!hasBlankRow(valuesColumn))
                                        {
                                            requirements.add(valuesColumn);
                                        }

                                    }

                                }
                                else  //if it is a requirement with more than one paragraph in the desc
                                {   //Each list of the multipleLines only contain two elements the description(0) and the format (1)
//                                    System.out.println("D");
                                    for(int k=0 ; k<multipleLines.size(); k++) //  ArrayList<String> singleLine : multipleLines)
                                    {
                                        if(multipleLines.get(k).size() == 2) //each list contains 2 elements the format and the req description
                                        {
                                            Map preprocessData = PreprocessingReq.preprocessing(multipleLines.get(k).get(0));  //description
                                            String descriptionMult = (String)preprocessData.get("text");
                                            String[] listDescr = descriptionMult.split("\n");
                                            if(listDescr.length > 1) // when the paragraph contains phrases and sentences
                                            {
//                                                System.out.println("E");
                                                for(int l=0; l<listDescr.length; l++)
                                                {
                                                    ArrayList<String> singleLine = new ArrayList<String>();
                                                    singleLine.add(headerTable);
//                                                    System.out.println("error");
                                                    singleLine.add(reqIdTable+"-B"+(k+1)+"-"+(l+1));
                                                    singleLine.add(listDescr[l]); //split text
                                                    singleLine.add(String.valueOf(preprocessData.get("boolean"))); // if it has been preprocessed
                                                    singleLine.add(multipleLines.get(k).get(0));
                                                    singleLine.add(multipleLines.get(k).get(1));//format
                                                    int wordsReq = countWords(listDescr[l]);
                                                    singleLine.add(String.valueOf(wordsReq));
                                                    singleLine.add(rationalTable);
                                                    //adding multiple requirements
                                                    if(!hasBlankRow(singleLine))
                                                    {
                                                        requirements.add(singleLine);
                                                    }
                                                }

                                            }else // when the paragraph does not have a point
                                            {
//                                                System.out.println("F");
                                                ArrayList<String> singleLine = new ArrayList<String>();

                                                singleLine.add(headerTable);
                                                singleLine.add(reqIdTable+"-"+(k+1));

                                                //Map preprocessData = preprocessing(multipleLines.get(k).get(0));  //description
                                                singleLine.add((String)preprocessData.get("text")); //preprocessed
                                                singleLine.add(String.valueOf(preprocessData.get("boolean")));
                                                singleLine.add(multipleLines.get(k).get(0));
                                                singleLine.add(multipleLines.get(k).get(1));//format
                                                int wordsReq = countWords((String)preprocessData.get("text"));
                                                singleLine.add(String.valueOf(wordsReq));

                                                singleLine.add(rationalTable);
                                                //adding multiple requirements
                                                if(!hasBlankRow(singleLine))
                                                {
                                                    requirements.add(singleLine);
                                                }

                                            }

                                        }else
                                        {
                                            ArrayList<String> singleLine = new ArrayList<String>();

                                            singleLine.add(headerTable);
                                            singleLine.add(reqIdTable+(k));
                                            singleLine.add("error");
                                            singleLine.add("error");
                                            singleLine.add("error");
                                            singleLine.add("error");
                                            singleLine.add("error");
                                            singleLine.add(rationalTable);
                                            //adding multiple requirements
                                            if(!hasBlankRow(singleLine))
                                            {
                                                requirements.add(singleLine);
                                            }
                                        }
                                    }
                                }

//                                System.out.println("Alvaro");
//                                System.out.println(valuesColumn.size());
//                                for(int k=0; k<= valuesColumn.size(); k++)
//                                {
//                                    System.out.println(valuesColumn.get(k));
//                                }

                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return requirements;
    }


    public static Boolean isSectionFuncReq(IBodyElement elem, XWPFStyles styles, Boolean isFuncRequirement )
    {
        String nameStyle = null;

        if (elem instanceof XWPFParagraph)
        {
            String style = ((XWPFParagraph) elem).getStyleID();
            XWPFStyle paragraphStyle =  styles.getStyle(style);

            if(paragraphStyle != null)
            {

                if(fileContainRequirements((XWPFParagraph) elem))
                {
                    nameStyle = paragraphStyle.getName().toLowerCase().trim(); //Heading1 or Heading2
                    isFuncRequirement = true;


                }else if(readWordFile.getHeadingForRequirements(paragraphStyle).equals("heading 1"))
                {
                    isFuncRequirement = false;
                }
            }
        }
        return isFuncRequirement;
    }




    public static int countWords(String requirement)
    {
        String requ_trimmed = requirement.trim();
        if (requ_trimmed.length() == 0)
            return 0;
        //return requ_trimmed.split("\\W+").length;
        return requ_trimmed.split(" ").length;

    }

    /*
    * We have two methods It includes metadata
    * a) Method that split each requirement into clauses
    * b) Method that generate the metadata an per each row is a requirement
    * */
    public static void main(String args[])
    {
        File folder = new File(pathWordDocuments);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {

            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                String nameFile = listOfFiles[i].getName();
                List<List<String>> reqInDataset = getFuncRequirements(folder+"/"+nameFile);
                System.out.println("Number of Requ:"+reqInDataset.size());
                createCsv(reqInDataset, folder+"/"+nameFile );

            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
    }

}
