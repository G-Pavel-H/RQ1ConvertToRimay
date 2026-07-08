import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreprocessingReq
{
    public static Map preprocessing(String originalText)
    {
        Boolean change = false;
        Map dataReturn = new HashMap();
        String preprocessedText = originalText.trim();
        //preprocessedText = removePath(originalText);
        //preprocessedText = removePath2(preprocessedText);
        //preprocessedText = removeTextParentheses(originalText);
        //preprocessedText = removeAdditionalInfo(preprocessedText);

        //Label the punto seguido whenever is found in a sentence
        /* Deactivated for now because it was causing mistakes
        Map SplitText = SplitTextPoints(preprocessedText);
        Boolean SplitInText = (Boolean)SplitText.get("boolean");
        preprocessedText = (String)SplitText.get("text"); */

        //Replace the colon in the following case :DS:
        Map removeColon = RemoveColonAttribute(preprocessedText);
        Boolean colonInWord = (Boolean)removeColon.get("boolean");
        preprocessedText = (String)removeColon.get("text");

        //Replace the semicolon in the following cas ;Attribute:
        //Checked the grouping categories
        Map removeSemicolon = RemoveSemiColonAttribute(preprocessedText);
        Boolean semicolonInWord = (Boolean)removeSemicolon.get("boolean");
        preprocessedText = (String)removeSemicolon.get("text");

        //Remove the text that it is between the parenthesis  (ADDITIONAL TEXT)
        //Considered as additional text
        Map textParenthesis = RemoveParenthesisText(preprocessedText);
        Boolean textInParenthesis = (Boolean)textParenthesis.get("boolean");
        preprocessedText = (String)textParenthesis.get("text");

        //Remove

        //Map textExamples = removeExamples(preprocessedText);
        //Boolean textInExamples = (Boolean)(textExamples.get("boolean"));
        //preprocessedText = (String)textExamples.get("text");

        //Map textNotes    = removeNotes(preprocessedText);
        //Boolean textInnNotes = (Boolean)textNotes.get("boolean");
        //preprocessedText = (String)textNotes.get("text");

        //Map textIllustrate = removeToIllustrate(preprocessedText);
        //Boolean textInIllustrate = (Boolean)textIllustrate.get("boolean");
        //preprocessedText = (String)textIllustrate.get("text");

        //Check the Capturing groups
        preprocessedText = RemoveMulBreakLines(preprocessedText);
        preprocessedText = RemoveLastBreakLine(preprocessedText);

        //System.out.println(preprocessedText);
        //preprocessedText = removePointsCommas(preprocessedText);
        //System.out.println(preprocessedText);
        //preprocessedText = removeDoubleQuotes(preprocessedText);
        //System.out.println(preprocessedText);
        preprocessedText = removeDoubleSpaces(preprocessedText);

        //if(textInExamples == true || textInnNotes==true || textInIllustrate==true || SplitInText==true)
        if(  colonInWord==true || textInParenthesis==true || semicolonInWord==true)
        {
            change = true;
        }
        dataReturn.put("boolean", change);
        dataReturn.put("text",preprocessedText);

        return dataReturn;
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


    public static Map removeToIllustrate(String text)
    {
        String textAfter = text;
        Boolean textRemoved = false;
        Map dataToReturn = new HashMap();
        //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try{
            String pattern = "(?i)(\\bTo illustrate\\b)(.*\\.)";
            Pattern p = Pattern.compile("(?i)(\\bTo illustrate\\b)(.*\\.)");
            Matcher m = p.matcher(text);
            if(m.find()){
                //System.out.println("There is an To Illustrate in the requirement please check: ");
                //System.out.println(text);
                //String yesNo = reader.readLine();

                //if(yesNo.trim().equals("yes"))
                //{
                //String regexPlusInfoOne = "(?i)(\\bTo illustrate\\b)(.*|.*\\.)";
                textAfter = text.replaceAll(pattern,"");
                textRemoved = true;

                //    System.out.println( textAfter );
                // }
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        dataToReturn.put("boolean", textRemoved );
        dataToReturn.put("text", textAfter);
        return dataToReturn;
    }


    public static Map removeExamples(String text)
    {
        String textAfter = text;
        Map dataToReturn = new HashMap();


        Boolean textRemoved = false;
        //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try{

            String pattern = "(?i)(\\bFor example\\b|\\bExemple\\b|\\bExample\\b)(.*\\.)";
            Pattern p = Pattern.compile("(?i)(\\bFor example\\b|\\bExemple\\b|\\bExample\\b)(.*\\.)");
            Matcher m = p.matcher(text);

            if(m.find())
            {
                //System.out.println("There is an example in the requirement please check: ");
                //System.out.println(text);
                //String yesNo = reader.readLine();
                //if(yesNo.trim().equals("yes"))
                //{
                textAfter = text.replaceAll(pattern,"");
                textRemoved = true;
                //System.out.println( textAfter );
                //}
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        dataToReturn.put("boolean", textRemoved);
        dataToReturn.put("text",textAfter);
        return dataToReturn;
    }
     /*
    *
    *   Pattern p = Pattern.compile("(?i)(\\bFor example\\b|\\bExemple\\b|\\bExample\\b)(.*|.*\\.)");
        Matcher m = p.matcher(content);
        if (m.find()){
    *
    * */

    public static Map removeNotes(String text)
    {
        String textAfter = text;
        Boolean textRemoved = false;
        Map dataToReturn = new HashMap();
        //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try{
            String pattern = "(\\bNotes\\b|\\bNote\\b).*\\.";
            Pattern p = Pattern.compile("(\\bNotes\\b|\\bNote\\b).*\\.");
            Matcher m = p.matcher(text);
            if(m.find())
            {
                //System.out.println("There are Notes in the requirement please check: ");
                //System.out.println( text);
                //String yesNo = reader.readLine();
                //if(yesNo.trim().equals("yes"))
                //{
                textAfter = text.replaceAll(pattern,"");
                textRemoved = true;

                //System.out.println( textAfter );
                //}
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        dataToReturn.put("boolean",textRemoved);
        dataToReturn.put("text",textAfter);
        return dataToReturn;
    }


    public static String RemoveMulBreakLines(String text)
    {
        String textAfter = text;
        String pattern = "(\\n{2,})([A-z0-9]+)";
        Pattern p = Pattern.compile("(\\n{2,})([A-z0-9]+)");
        Matcher m = p.matcher(text);
        if(m.find())
        {
            textAfter = text.replaceAll(pattern,".<lb>$2");
        }
        return textAfter;
    }

    public static String RemoveLastBreakLine(String text)
    {
        //\n*$
        String textAfter = text;
        String pattern = "\\n*$";
        Pattern p = Pattern.compile("\\n*$");
        Matcher m = p.matcher(text);
        if(m.find())
        {
            textAfter = text.replaceAll(pattern,"");
        }
        return textAfter;
    }


    //In this section, the original instruction is defined by the retrieved “CBL TA-LuxCSD FIA ” instruction. Notes sdasdas.
    public static Map SplitTextPoints(String text)
    {
        String textAfter = text;
        Boolean textRemoved = false;
        Map dataToReturn = new HashMap();
        try{
            //String pattern = "(\\w{3,})\\.(\\s+)";
            String pattern = "(\\w{6,})\\.(\\x20+)(\\w{2,})";
            Pattern p = Pattern.compile("(\\w{6,})\\.(\\x20+)(\\w{2,})");
            Matcher m = p.matcher(text);
            if(m.find())
            {
                //System.out.println("Before: "+text);
                //textAfter = text.replaceAll(pattern,"$1.\n$2");
                textAfter = text.replaceAll(pattern,"$1.<period>$3");
                textRemoved = true;
                //System.out.println("After: "+textAfter);
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        dataToReturn.put("boolean",textRemoved);
        dataToReturn.put("text",textAfter);
        return dataToReturn;
    }



    //;83a:   :453sd:
    public static Map RemoveColonAttribute(String text)
    {
        String textAfter = text;
        Boolean textRemoved = false;
        Map dataToReturn = new HashMap();
        try{
            String pattern = "(\\x3A)(\\w+)(\\x3A)";
            Pattern p = Pattern.compile("(\\x3A)(\\w+)(\\x3A)");
            Matcher m = p.matcher(text);
            if(m.find())
            {
                textAfter = text.replaceAll(pattern," $2 ");
                textRemoved = true;
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        dataToReturn.put("boolean",textRemoved);
        dataToReturn.put("text",textAfter);
        return dataToReturn;

    }

    public static Map RemoveSemiColonAttribute(String text)
    {
        String textAfter = text;
        Boolean textRemoved = false;
        Map dataToReturn = new HashMap();
        try{
            String pattern = "(\\x3B)(\\w+)(\\x3A)";
            Pattern p = Pattern.compile("(\\x3B)(\\w+)(\\x3A)");
            Matcher m = p.matcher(text);
            if(m.find())
            {
                textAfter = text.replaceAll(pattern," $2 ");
                textRemoved = true;
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        dataToReturn.put("boolean",textRemoved);
        dataToReturn.put("text",textAfter);
        return dataToReturn;

    }


    public static Map RemoveParenthesisText(String text)
    {
        String textAfter = text;
        Boolean textRemoved = false;
        Map dataToReturn = new HashMap();
        try{
            String pattern = "(\\x28.*?\\x29)";
            Pattern p = Pattern.compile("(\\x28.*?\\x29)");
            Matcher m = p.matcher(text);
            if(m.find())
            {
                textAfter = text.replaceAll(pattern," ");
                textRemoved = true;
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        dataToReturn.put("boolean",textRemoved);
        dataToReturn.put("text",textAfter);
        return dataToReturn;
    }
}
