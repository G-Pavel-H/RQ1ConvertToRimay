import java.io.*;
import java.util.*;
import java.util.Iterator;
import com.opencsv.CSVWriter;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.openxml4j.opc.*;


public class readWordFileSoftPreprocessing {

    public static String pathWordDocuments = "path to be updated";
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

            containRequirements = true;

        }
        return containRequirements;
    }

    //public static Boolean hasBlankRow(String[] rowData)
    public static Boolean hasBlankRow(ArrayList<String> rowData)
    {
        boolean flag = false;
        int noReqColumns = rowData.size();



        int counterBlanksColumns = 0;
        for(String el : rowData)
        {

            if(el.trim().equalsIgnoreCase("<blank>"))
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

                                for (int j = 0; j < ((((XWPFTable) elem).getRow(i).getTableCells().size())); j++)
                                {
                                    if(!checkEmptyField(((XWPFTable) elem).getRow(i).getCell(j).getText()))
                                    {

                                        List<XWPFParagraph> cellParagraphs =  ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();



                                        if(cellParagraphs.size()>1)
                                        {

                                            if((header.getCell(j).getText().trim().toLowerCase()).equals("description"))
                                            {
                                                Map singleText = convertToSingleParagraph(cellParagraphs);
                                                Map preprocessData = PreprocessingReq.preprocessing((String)singleText.get("singleText"));

                                                valuesColumn.add((String)preprocessData.get("text")); //preprocessed
                                                valuesColumn.add(String.valueOf(preprocessData.get("boolean")));
                                                valuesColumn.add((String)singleText.get("singleText"));
                                                valuesColumn.add((String)singleText.get("listFormats"));
                                                int wordsReq = countWords((String)preprocessData.get("text"));
                                                valuesColumn.add(String.valueOf(wordsReq));

                                            }else //other columns
                                            {
                                                Map singleText = convertToSingleParagraph(cellParagraphs);

                                                valuesColumn.add((String)singleText.get("singleText"));
                                            }
                                        }else //no paragraphs
                                        {

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

    public static Map convertToSingleParagraph(List<XWPFParagraph> paragraphs)
    {
        String singleText = new String();
        List<String> listFormats=new ArrayList<String>();
        for(int i=0; i<paragraphs.size(); i++)
        {
            //


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

        dataToReturn.put("singleText", singleText );
        dataToReturn.put("listFormats", formats);
        return dataToReturn;
    }


    public static Boolean hasMultipleParagraphs(XWPFTable elem, int row, int cell)
    {
        Boolean multipleParagraphsDescription = false;
        List<XWPFParagraph> cellParagraphs = elem.getRow(row).getCell(cell).getParagraphs();

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

                                                headerTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                                break;
                                            case "req. id":

                                                reqIdTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();

                                                break;
                                            case "description":

                                                multipleParagraphsDescription = hasMultipleParagraphs((XWPFTable) elem,i,j);
                                                if(multipleParagraphsDescription)
                                                {
                                                    List<XWPFParagraph> cellParagraphs = ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();
                                                    multipleLines = convertToMultipleLines(cellParagraphs);
                                                    descTable = "multiple";
                                                } else {
                                                    descTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                                }

                                                break;
                                            case "rational":

                                                multipleParagraphsRational = hasMultipleParagraphs((XWPFTable) elem,i,j);
                                                if(multipleParagraphsRational){
                                                    List<XWPFParagraph> cellParagraphs = ((XWPFTable) elem).getRow(i).getCell(j).getParagraphs();
                                                    Map singleText = convertToSingleParagraph(cellParagraphs);
                                                    rationalTable = (String) singleText.get("singleText");
                                                }else{
                                                    rationalTable = ((XWPFTable) elem).getRow(i).getCell(j).getText();
                                                }
                                                break;
                                        }
                                    }
                                }

                                if(! multipleParagraphsDescription){ //if it is a requirement with one paragraph in the desc

                                    Map preprocessData = PreprocessingReq.preprocessing(descTable);
                                    String descrReq = (String)preprocessData.get("text");
                                    String[] listPhrasesInReq = descrReq.split("\n");
                                    if(listPhrasesInReq.length>1) //If within the requirement it has multiple lines
                                    {

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
                                        valuesColumn.add(headerTable);
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
                                {

                                    for(int k=0 ; k<multipleLines.size(); k++)
                                    {
                                        if(multipleLines.get(k).size() == 2)
                                        {
                                            Map preprocessData = PreprocessingReq.preprocessing(multipleLines.get(k).get(0));  //description
                                            String descriptionMult = (String)preprocessData.get("text");
                                            String[] listDescr = descriptionMult.split("\n");
                                            if(listDescr.length > 1) // when the paragraph contains phrases and sentences
                                            {

                                                for(int l=0; l<listDescr.length; l++)
                                                {
                                                    ArrayList<String> singleLine = new ArrayList<String>();
                                                    singleLine.add(headerTable);

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

                }else if(getHeadingForRequirements(paragraphStyle).equals("heading 1"))
                {
                    isFuncRequirement = false;
                }
            }
        }
        return isFuncRequirement;
    }

    public static String getHeadingForRequirements(XWPFStyle paragraphStyle)
    {
        String style = paragraphStyle.getName().toLowerCase().trim();
        return style;
    }


    public static int countWords(String requirement)
    {
        String requ_trimmed = requirement.trim();
        if (requ_trimmed.length() == 0)
            return 0;
        //return requ_trimmed.split("\\W+").length;
        return requ_trimmed.split(" ").length;

    }


}
