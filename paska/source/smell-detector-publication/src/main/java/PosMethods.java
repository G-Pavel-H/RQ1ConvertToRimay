import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import java.io.StringReader;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.*;

public class PosMethods {
    //Method for regular expression
    public static Boolean MatchRegExpression(String text, String regex)
    {
        Boolean matchRegex = false;
        try{
            Pattern p = Pattern.compile(regex);

            Matcher m = p.matcher(text);
            if(m.find())
            {
                matchRegex = true;
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return matchRegex;
    }

    //returns a tagged sentence from a String
    public static String getTaggedTextFromSentence(ArrayList<TaggedWord> sentenceTag){
        String text = "";
        for(TaggedWord word:sentenceTag){
            text += word.toString()+" ";
        }
        return text.trim(); //.toLowerCase().trim();

    }

    //return a string from an array of tagged words
    public static String getTextFromSentence(ArrayList<TaggedWord> sentenceTag){
        String text = "";
        for(TaggedWord word:sentenceTag){
            text += word.word()+" ";
        }
        return text.trim(); //.toLowerCase().trim();
    }

    //returns the position of the first modal verb
    public static int firstModalVerb(ArrayList<TaggedWord> sentenceTag){
        int posModalVerb = -1;
        for(int i=0; i<sentenceTag.size();i++){

            if(sentenceTag.get(i).tag().contains("MD")){
                if(sentenceTag.get(i).word().toLowerCase().contains("must") || sentenceTag.get(i).word().toLowerCase().contains("shall")||

                        sentenceTag.get(i).word().toLowerCase().contains("will") || sentenceTag.get(i).word().toLowerCase().contains("should") ){

                    posModalVerb = i;
                    break;
                }
            }
        }
        return posModalVerb;
    }


    //returns true if there is a subordinated conjunction
    public static Boolean hasSubConj(ArrayList<TaggedWord>  sentenceTag)
    {
        boolean hasSubConj = false;
        String sentence = getTaggedTextFromSentence(sentenceTag);
        if(sentence.contains("/WRB") || sentence.contains("/WHADVP") || sentenceTag.toString().contains("/IN"))
        {

            String patternSub = "\\b(When|when|where|Where|While|while|if|If|IF|Until|until)\\b";
            if(MatchRegExpression(sentence, patternSub))
            {

                hasSubConj = true;
            }
        }
        return hasSubConj;
    }

    //Returns true if there is a modal verb
    public static Boolean hasModalVerb(ArrayList<TaggedWord>  w){
        boolean hasModVerb = false;
        String sentence = getTaggedTextFromSentence(w);
        if(sentence.contains("MD")){
            String patternSub = "\\b(must|should|shall|will)\\b";
            if(MatchRegExpression(sentence.toLowerCase(), patternSub)){
                hasModVerb = true;
            }
        }
        return  hasModVerb;
    }


    //checks for symbols that denote actions
    public static int firstSymbolCond(ArrayList<TaggedWord>  sentenceTag){
        int posSubConj = -1;
        for(int i=0;i<sentenceTag.size(); i++){
            String patternSub = "(=|<|>|\\u2260)";
            if(MatchRegExpression(sentenceTag.get(i).toString(), patternSub)){
                posSubConj = i;
                break;
            }
        }
        return posSubConj;
    }

    //Method that checks for the phrase "in case that"
    public static Integer posFirstPrepCondS8Sub(ArrayList<TaggedWord> segmentTagged) {
        int posFirstPrep = -1;
        int firstKeyPos = PosMethods.getPositionContains(segmentTagged,"in/IN");;
        int secondKeyPos = PosMethods.getPositionContains(segmentTagged, "case/NN");
        int thirdKeyPos = PosMethods.getPositionContains(segmentTagged, "that/IN");
        if(firstKeyPos!=-1 && secondKeyPos != -1 && thirdKeyPos != -1){
            if(firstKeyPos < secondKeyPos && secondKeyPos < thirdKeyPos){
                //Check that there are in a sequence
                if((secondKeyPos - firstKeyPos) == 1 && (thirdKeyPos - secondKeyPos)== 1){
                    posFirstPrep = firstKeyPos;
                }
            }
        }
        return  posFirstPrep;
    }

    //returns the position of subordinated conjunction
    public static int firstPosSubConj(ArrayList<TaggedWord>  sentenceTag){
        int posSubConj = -1;
        for(int i=0;i<sentenceTag.size(); i++){
            if(sentenceTag.get(i).tag().contains("WRB") || sentenceTag.get(i).tag().contains("WHADVP") || sentenceTag.toString().contains("IN")){

                String patternSub = "\\b(When|when|where|Where|While|while|if|If|IF|Until|until|Once|once)\\b";
                if(MatchRegExpression(sentenceTag.get(i).toString(), patternSub))
                {

                    posSubConj = i;
                    break;
                }
            }
        }
        return posSubConj;
    }

    //returns the first position of a preposition in a condition
    public static int firstPosPrepCond(ArrayList<TaggedWord> sentence){
        int posPrepCond = -1;
        for(int i=0; i < sentence.size(); i++){
            if(sentence.get(i).tag().contains("IN")){
                String patternPrep = "\\b(Upon|upon|On|on|After|after|Before|before)\\b"; //At|at|
                if(MatchRegExpression(sentence.get(i).toString(), patternPrep)){
                    posPrepCond = i;
                    break;
                }
            }
        }
        return posPrepCond;
    }

    //returns the first position of a preposition in a condition which is incomplete
    public static int firstPosPrepCondIncomp(ArrayList<TaggedWord> sentence){
        int posPrepCond = -1;
        for(int i=0; i < sentence.size(); i++){
            String patternPrep = "\\b(After|after|Before|before)\\b";
            if(MatchRegExpression(sentence.get(i).toString(), patternPrep)){
                posPrepCond = i;
                break;
            }
        }
        return posPrepCond;
    }


    //Method that remove blank spaces
    public static String removeBlankSpaces(String req){

        return req.replaceAll("\\x20{2,}"," ");
    }

    //Method that splits bullets points
    public static String separateBullet(String s){

        String newStr = s;
//        /(\x2A)(i|I\w+)
        try{
            Pattern p = Pattern.compile("(\\x2A)(\\w+)");
//            Pattern p = Pattern.compile("(\\x2A)(i|I\\w+)");
            Matcher m = p.matcher(s);

            if(m.find())
            {
                newStr = m.replaceAll("$1 $2");
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return newStr;



    }

    //join text that contain bullet points

    public static String joinBullet(String s){
        String newStr = s;

        try{
            Pattern p = Pattern.compile("(\\x2A)(\\x20)(\\w+)");
            Matcher m = p.matcher(s);
            if(m.find())
            {
                newStr = m.replaceAll("$1$3");
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return newStr;

    }

    //remove no break character
    public static String removeNoBreakCharacter(String w){

        return w.replaceAll("\\xA0","");


    }

    //checks if a segment contains a subordinated conjunction
    public static Boolean hasSubConjWord(TaggedWord  sentenceTag)
    {

        boolean hasSubConj = false;
        if(sentenceTag.toString().contains("/WRB") || sentenceTag.toString().contains("/WHADVP") || sentenceTag.toString().contains("/IN"))
        {
            String patternSub = "\\b(When|when|where|Where|While|while|if|If|IF|Until|until)\\b";
            if(MatchRegExpression(sentenceTag.toString(), patternSub))
            {

                hasSubConj = true;
            }
        }
        return hasSubConj;
    }

    //checks if a condition has a preposition
    public  static boolean hasPrepInCondition(ArrayList<TaggedWord> sentenceTag)
    {
        boolean hasPreposition = false;
        String sentence = getTaggedTextFromSentence(sentenceTag);

        if( sentence.contains("/IN"))
        {
            String patternSub = "\\b(Upon|upon|After|after|Before|before|On|on)\\b"; //|At|at|On|on removed
            if(MatchRegExpression(sentence, patternSub))
            {
                hasPreposition = true;
            }
        }
        return hasPreposition;
    }

    //get the type of rimay condition
    public static String getTypeConditionCsv(String verbCondition, String pennTreeBank)
    {
        String typeCondition = "";
        if(verbCondition.length()>1)
        {
            String patternVerbCond = "\\b(is|are|equal|equals|be|has|have|contain|contains)\\b";
            if(MatchRegExpression(verbCondition, patternVerbCond))
            {
                typeCondition = "logicalExpression";
            }else
            {
                typeCondition = "trigger";
            }
        }
        return typeCondition;
    }


    //return true if a list contains a given word
    public static Boolean listHasText(List<TaggedWord> wordsCond, String subString)
    {
        boolean contains = false;
        for (TaggedWord word:wordsCond)
        {
            if (word.value().contains(subString))
            {
                contains = true;
            }
        }
        return contains;
    }


    //get verb of verb phrase
    public static String getVerbIntent(ArrayList<TaggedWord> posVP, int numb_verbs, String reqID)
    {
        String verbIntent = "";
        if(numb_verbs == 1)
        {
            verbIntent = posVP.get(0).value();
        }

        if (numb_verbs == 4) {
            if ((listHasText(posVP,"has") || listHasText(posVP,"have") || listHasText(posVP,"had") ) && listHasText(posVP,"been")){
                verbIntent = posVP.get(3).value();
            }else if ((listHasText(posVP,"is") || listHasText(posVP,"are")) && (listHasText(posVP,"be") || listHasText(posVP,"been"))){
                verbIntent = posVP.get(3).value();
            }else if ((listHasText(posVP,"is") || listHasText(posVP,"are") || listHasText(posVP,"was")|| listHasText(posVP,"were")) && listHasText(posVP,"being") ){
                verbIntent = posVP.get(3).value();
            }else{
                verbIntent = posVP.get(0).value();
            }

        }


        if(numb_verbs == 3){
            if( (listHasText(posVP,"has") || listHasText(posVP,"have")) && (listHasText(posVP,"been") || listHasText(posVP,"be") ||  listHasText(posVP,"successfully")) ){
                verbIntent = posVP.get(2).value();
            }else if( (listHasText(posVP,"does") || listHasText(posVP,"do")) &&  listHasText(posVP,"no")){
                verbIntent = posVP.get(2).value();
            }else if((listHasText(posVP,"wants") || listHasText(posVP,"want")) &&  listHasText(posVP,"to")){
                verbIntent = posVP.get(2).value();
            }else{
                verbIntent = posVP.get(0).value();
            }

        }



        if(numb_verbs == 2)
        {
            if(listHasText(posVP,"is") || listHasText(posVP,"are") || listHasText(posVP,"was") || listHasText(posVP,"were")){
                verbIntent = posVP.get(1).value();
            }else{
                verbIntent = posVP.get(0).value();
            }

        }
        return verbIntent;
    }





    //Get the verb phrase
    public static ArrayList<TaggedWord> getPosVPClause(int numVerbs, ArrayList<TaggedWord> taggedVP)
    {
        ArrayList <TaggedWord> vpCond = new ArrayList<>();

        for(int i=0; i<numVerbs; i++)
        {
            vpCond.add(taggedVP.get(i));
        }
        return vpCond;
    }



    //return number of verbs
    public static Integer getNumbersVerbsClause(ArrayList<TaggedWord> wordsVPhrase)
    {
        int numberVerbsVPCond = 0;
//        String[] wordsVP = taggedVerbPhrase.split(" ");

        if (wordsVPhrase.size() > 3) //4 words or more
        {
            //position[1] something that is not verb
            if(wordsVPhrase.get(0).tag().contains("VB") && !(wordsVPhrase.get(1).tag().contains("VB")) &&  wordsVPhrase.get(2).tag().contains("VB") && wordsVPhrase.get(3).tag().contains("VB"))
            {
                numberVerbsVPCond = 4;
            }//Position [2] something that is not verb
            else if(wordsVPhrase.get(0).tag().contains("VB") && wordsVPhrase.get(1).tag().contains("VB") && !(wordsVPhrase.get(2).tag().contains("VB")) && wordsVPhrase.get(3).tag().contains("VB"))
            {
                numberVerbsVPCond = 4;
            }else if (wordsVPhrase.get(0).tag().contains("VB") && wordsVPhrase.get(1).tag().contains("VB") && wordsVPhrase.get(2).tag().contains("VB"))//first word
            {
                numberVerbsVPCond = 3;
            }else if (wordsVPhrase.get(0).tag().contains("VB") && wordsVPhrase.get(1).tag().contains("VB") && !(wordsVPhrase.get(2).tag().contains("VB")) && !(wordsVPhrase.get(3).tag().contains("VB")))
            {
                numberVerbsVPCond = 2;
            }//Position [1] not a VERB
            else if (wordsVPhrase.get(0).tag().contains("VB") && !(wordsVPhrase.get(1).tag().contains("VB")) && wordsVPhrase.get(2).tag().contains("VB") && !(wordsVPhrase.get(3).tag().contains("VB")))
            {
                numberVerbsVPCond = 3;
            }else if (wordsVPhrase.get(0).tag().contains("VB") && !(wordsVPhrase.get(1).tag().contains("VB")) && !(wordsVPhrase.get(2).tag().contains("VB")) )
            {
                numberVerbsVPCond = 1;
            }
        }else if(wordsVPhrase.size() == 3) // 2 or 3 words
        {
            if(wordsVPhrase.get(0).tag().contains("VB") && wordsVPhrase.get(1).tag().contains("VB") && (wordsVPhrase.get(2).tag().contains("VB")))
            {
                numberVerbsVPCond = 3;
            }
            else if (wordsVPhrase.get(0).tag().contains("VB") && wordsVPhrase.get(1).tag().contains("VB") && !(wordsVPhrase.get(2).tag().contains("VB")))//first word
            {
                numberVerbsVPCond = 2;
            }else if(wordsVPhrase.get(0).tag().contains("VB") && !wordsVPhrase.get(1).tag().contains("VB") && (wordsVPhrase.get(2).tag().contains("VB")))
            {
                numberVerbsVPCond = 3;
            }else if(wordsVPhrase.get(0).tag().contains("VB") && !wordsVPhrase.get(1).tag().contains("VB") && !(wordsVPhrase.get(2).tag().contains("VB")))
            {
                numberVerbsVPCond = 1;
            }

        }else if (wordsVPhrase.size() == 2)
        {
            if (wordsVPhrase.get(0).tag().contains("VB") && wordsVPhrase.get(1).tag().contains("VB"))
            {
                numberVerbsVPCond = 2;

            }else if (wordsVPhrase.get(0).tag().contains("VB") && !wordsVPhrase.get(1).tag().contains("VB"))
            {
                numberVerbsVPCond = 1;
            }

        }else if(wordsVPhrase.size() == 1)
        {
            if (wordsVPhrase.get(0).tag().contains("VB"))
            {
                numberVerbsVPCond = 1;
            }
        }
        return  numberVerbsVPCond;
    }

    //returns the verb of a System response
    public static String getVerbSR(ArrayList<TaggedWord> taggedVerbPhraseSR)
    {
        String verbSR = "";
        int numberVerbsSR = 0;
        if (taggedVerbPhraseSR.size() > 3) //4 words or more
        {

            if(taggedVerbPhraseSR.get(0).tag().contains("VB") && !(taggedVerbPhraseSR.get(1).tag().contains("VB")) &&  !taggedVerbPhraseSR.get(2).tag().contains("VB") && !taggedVerbPhraseSR.get(3).tag().contains("VB"))
            {
                numberVerbsSR = 1;
                verbSR = taggedVerbPhraseSR.get(0).value();
            }
            else if(!taggedVerbPhraseSR.get(0).tag().contains("VB") && taggedVerbPhraseSR.get(1).tag().contains("VB") && !(taggedVerbPhraseSR.get(2).tag().contains("VB")) && !taggedVerbPhraseSR.get(3).tag().contains("VB"))
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }else if (taggedVerbPhraseSR.get(0).value().contains("be") && taggedVerbPhraseSR.get(1).value().contains("able") && taggedVerbPhraseSR.get(2).value().contains("to") && taggedVerbPhraseSR.get(3).tag().contains("VB"))//first word
            {
                numberVerbsSR = 4;
                verbSR = taggedVerbPhraseSR.get(3).value();
            }else if (taggedVerbPhraseSR.get(0).value().contains("be") && taggedVerbPhraseSR.get(1).tag().contains("VB") && (taggedVerbPhraseSR.get(2).value().contains("based")) && !(taggedVerbPhraseSR.get(3).tag().contains("VB")))
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }//Position [1] not a VERB
            else if (taggedVerbPhraseSR.get(0).value().contains("be") && (taggedVerbPhraseSR.get(1).tag().contains("VB")) && !taggedVerbPhraseSR.get(2).tag().contains("VB") && !(taggedVerbPhraseSR.get(3).tag().contains("VB")))
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }else if (taggedVerbPhraseSR.get(0).value().contains("be") && !(taggedVerbPhraseSR.get(1).tag().contains("VB")) && taggedVerbPhraseSR.get(2).tag().contains("VB") && !(taggedVerbPhraseSR.get(3).tag().contains("VB")))
            {
                numberVerbsSR = 3;
                verbSR = taggedVerbPhraseSR.get(2).value();
            }else//In case we assume that the first will be the verb
            {
                numberVerbsSR = 1;
                verbSR = taggedVerbPhraseSR.get(0).value();
            }

        }else if(taggedVerbPhraseSR.size() == 3) // 2 or 3 words
        {
            if(taggedVerbPhraseSR.get(0).tag().contains("VB") && !taggedVerbPhraseSR.get(1).tag().contains("VB") && !(taggedVerbPhraseSR.get(2).tag().contains("VB")))
            {
                numberVerbsSR = 1;
                verbSR = taggedVerbPhraseSR.get(0).value();
            }
            else if (!taggedVerbPhraseSR.get(0).tag().contains("VB") && taggedVerbPhraseSR.get(1).tag().contains("VB") && !(taggedVerbPhraseSR.get(2).tag().contains("VB")))//first word
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }else if(taggedVerbPhraseSR.get(0).value().contains("be") && !taggedVerbPhraseSR.get(1).tag().contains("VB") && (taggedVerbPhraseSR.get(2).tag().contains("based")))
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }else if(taggedVerbPhraseSR.get(0).value().contains("be") && taggedVerbPhraseSR.get(1).tag().contains("VB") && !(taggedVerbPhraseSR.get(2).tag().contains("VB")))
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }else if(taggedVerbPhraseSR.get(0).value().contains("be") && !taggedVerbPhraseSR.get(1).tag().contains("VB") && (taggedVerbPhraseSR.get(2).tag().contains("VB")))
            {
                numberVerbsSR = 3;
                verbSR = taggedVerbPhraseSR.get(2).value();
            }else //In case we assume that the first will be the verb
            {
                numberVerbsSR = 1;
                verbSR = taggedVerbPhraseSR.get(0).value();
            }

        }else if (taggedVerbPhraseSR.size() == 2)
        {
            if (taggedVerbPhraseSR.get(0).tag().contains("VB") && !taggedVerbPhraseSR.get(1).tag().contains("VB"))
            {
                numberVerbsSR = 1;
                verbSR = taggedVerbPhraseSR.get(0).value();

            }else if (!taggedVerbPhraseSR.get(0).tag().contains("VB") && taggedVerbPhraseSR.get(1).tag().contains("VB"))
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }else if (taggedVerbPhraseSR.get(0).value().contains("be") && taggedVerbPhraseSR.get(1).tag().contains("VB"))
            {
                numberVerbsSR = 2;
                verbSR = taggedVerbPhraseSR.get(1).value();
            }else//In case we assume that the first will be the verb
            {
                numberVerbsSR = 1;
                verbSR = taggedVerbPhraseSR.get(0).value();
            }

        }else if(taggedVerbPhraseSR.size() == 1)
        {
            if (taggedVerbPhraseSR.get(0).tag().contains("VB"))
            {
                numberVerbsSR = 1;
                verbSR = taggedVerbPhraseSR.get(0).value();
            }
        }
        return verbSR;
    }

    //Converts a string into tagged sentence
    public static ArrayList<TaggedWord> getTagSentence(String segment, MaxentTagger tagger)
    {
        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(segment));
        dp.setTokenizerFactory(null); //Since the text is already tokenized it will just split using blank spaces

        ArrayList<TaggedWord> allSentences = new ArrayList<>();
        List<TaggedWord> tSentence =  new ArrayList<>();
        for(List<HasWord> sentence :dp){
            tSentence = tagger.tagSentence(sentence);
            allSentences.addAll(tSentence); //gathering all the sentences
        }

        return allSentences;
    }



    //Get the first position of  a word within a segment/clause
    public static Integer getPositionContains(ArrayList<TaggedWord> segmentTag, String keyword){
        //String[] wordSegments = segmentTag.split(" ");
        int indexKeyword = -1;
        for(int i=0; i< segmentTag.size(); i++){
            if(!segmentTag.get(i).word().contains("\\*")){
                if(segmentTag.get(i).toString().toLowerCase().contains(keyword.toLowerCase())){
                    indexKeyword = i;
                    break;
                }

            }

        }
        return indexKeyword;
    }

    //Finds  a specific word after word
    public static Integer getPosWordFromSpecLoc(ArrayList<TaggedWord> segmentTag, String keyword, int location){
        //String[] wordSegments = segmentTag.split(" ");
        int indexKeyword = -1;
        if(location < segmentTag.size()){

            if(location < segmentTag.size()){
                for(int i=location; i< segmentTag.size(); i++){
                    if(!segmentTag.get(i).word().contains("\\*")){
                        if(segmentTag.get(i).toString().toLowerCase().contains(keyword.toLowerCase())){
                            if(!segmentTag.get(i).toString().toLowerCase().contains("/vbg")){
                                indexKeyword = i;
                                break;
                            }

                        }
                    }
                }
            }
        }
        return indexKeyword;
    }

    //Exact match only for the verb
    public static Integer getPosVerbFromSpecLoc(ArrayList<TaggedWord> segmentTag, String keyword, int location){
        //String[] wordSegments = segmentTag.split(" ");
        int indexKeyword = -1;
        if(location < segmentTag.size()){

            if(location < segmentTag.size()){
                for(int i=location; i< segmentTag.size(); i++){
                    if(!segmentTag.get(i).word().contains("\\*")){
                        if(segmentTag.get(i).tag().toLowerCase().equals(keyword.toLowerCase())){
                            indexKeyword = i;
                            break;
                        }

                    }
                }
            }
        }
        return indexKeyword;
    }

    //get verb from a specific location
    public static String getVerbSpecLoc(ArrayList<TaggedWord> segmentTag, String keyword, int loc){
        String verb = "";
        if(loc < segmentTag.size()){
            for(int i=loc; i< segmentTag.size(); i++){
                if(segmentTag.get(i).tag().toLowerCase().equals(keyword.toLowerCase())){
                    verb = segmentTag.get(i).word();
                    break;
                }
            }
        }
        return verb;
    }


    //get the segment/clause that contains a keyword
    public static String getElementContains(ArrayList<TaggedWord> segmentTag, String keyword){
        String verb = "";
        for(int i=0; i< segmentTag.size(); i++){
            if(segmentTag.get(i).toString().toLowerCase().contains(keyword.toLowerCase())){
                verb = segmentTag.get(i).word();
                break;
            }
        }
        return verb;
    }


    public static String getElementSpecLoc(ArrayList<TaggedWord> segmentTag, String keyword, int loc){
        String verb = "";
        if(loc < segmentTag.size()){
            for(int i=loc; i< segmentTag.size(); i++){
                if(segmentTag.get(i).toString().toLowerCase().contains(keyword.toLowerCase())){
                    if(!segmentTag.get(i).toString().toLowerCase().contains("/vbg")){
                        verb = segmentTag.get(i).word();
                        break;
                    }

                }
            }
        }
        return verb;
    }

    //get the position of a word
    public static Integer getPositionExact(ArrayList<TaggedWord> segmentTag, String keyword){
        int indexKeyword = -1;
        for(int i=0; i< segmentTag.size(); i++){
            if(segmentTag.get(i).tag().equals(keyword)){
                indexKeyword = i;
                break;
            }
        }
        return indexKeyword;
    }


    //returns the frequency of word on a segment/clause
    public static Integer getFreqWord(ArrayList<TaggedWord> segments, String tag){
        int freq = 0;
        for(TaggedWord word: segments){
            if(word.toString().toLowerCase().contains(tag.toLowerCase())){
                freq ++;
            }
        }
        return freq;
    }

    //returns true if a segment is a scope
    public static boolean isSegmentScope(ArrayList<TaggedWord> segmentTag){ //ArrayList<TaggedWord>
        boolean isScope = false;
        int positionFirstKey = -1;
        int positionSecondKey = -1;
        if(!hasModalVerb (segmentTag) && !hasSubConj(segmentTag)){
            positionFirstKey = getPositionContains(segmentTag, "for/IN");
            positionSecondKey = getPositionContains(segmentTag, "/N");
        }
        if(positionFirstKey!=-1 && positionSecondKey!=-1){
            if(positionFirstKey < positionSecondKey){
                isScope = true;
            }
        }
        return isScope;
    }

    //return the smallest number of a list of numbers
    public static Integer getShortNumber(int[] indexes){
        int temp = 0;
        int shortNumber = -1;
        ArrayList<Integer> lIndexes = new ArrayList<>();
        for(int ind:indexes){
            if (ind>=0){
                lIndexes.add(ind);
            }
        }

        Object[] aIndexes = lIndexes.toArray();
        if (aIndexes.length>0){
            for(int i=0; i<aIndexes.length - 1; i++){
                for(int j=i+1; j< aIndexes.length ; j++){
                    if((Integer)aIndexes[i] > (Integer)aIndexes[j]){
                        temp= (Integer)aIndexes[i];
                        aIndexes[i] = aIndexes[j];
                        aIndexes[j] = temp;
                    }
                }
            }
            shortNumber = (Integer)aIndexes[0];
        }
        return shortNumber;
    }


    //returns if a scope is incorrect
    public static String isSegScopeIncorrect(ArrayList<TaggedWord> segmentTag){
        boolean isScopeNotCorr = false;
        boolean isScopeIncompl = false;
        String typeScope = "Other";
        int posFor = getPositionContains(segmentTag,"for/IN");
        int posNoun = getPositionContains(segmentTag,"/N");
        int posSubConjA = getPositionContains(segmentTag, "/WRB");
        int posSubConjB = getPositionContains(segmentTag,"/WHADVP");
        int posSubConjC = getPositionContains(segmentTag,"if/IN");
        int posModVerb = getPositionContains(segmentTag, "/MD");
        int posVerb = getPositionContains(segmentTag, "/VB");
        int[] posSubConjs= {posSubConjA,posSubConjB,posSubConjC};
        int posFirstSubConj = PosMethods.getShortNumber(posSubConjs);

        if(posFor!=-1  && posFirstSubConj!=-1 && posModVerb!=-1 ){//Contains For, when, and modal verb
            if(posNoun!=-1){
                if(posFor < posFirstSubConj && posFor< posModVerb && posFor < posNoun){
                    isScopeNotCorr = true;
                }
            }else{
                if(posFor < posFirstSubConj && posFor< posModVerb && posFor < posNoun) {
                    isScopeIncompl = true;
                }
            }
        }else if(posFor!=-1  && posFirstSubConj!=-1 && posModVerb==-1){ //Contains For, when, and NOT modal verb
            if(posNoun!=-1){
                if(posFor < posFirstSubConj && posFor < posNoun){
                    isScopeNotCorr = true;
                }
            }else {
                if(posFor < posFirstSubConj){
                    isScopeIncompl = true;
                }
            }
        }else if(posFor!=-1  && posFirstSubConj==-1 && posModVerb==-1){ //Contains For,NOT when, and NOT modal verb
            if(posNoun==-1){
                isScopeIncompl = true;
            }
        }else if(posFor!=-1  && posFirstSubConj==-1 && posModVerb!=-1 && posVerb == -1 ){ //Contains For, NOT when, and  modal verb
            if(posNoun!=-1){
                if(posFor < posModVerb && posFor < posNoun){
                    isScopeNotCorr = true;
                }
            }else {
                if(posFor < posModVerb){
                    isScopeIncompl = true;
                }
            }
        }
        if(isScopeNotCorr){
            typeScope = "ScopeIncorr";

        }else if(isScopeIncompl){
            typeScope = "ScopeIncompl";
        }

        return typeScope;
    }


    //return number of words after verb
    public static Integer getNumberWordAfterVerb(ArrayList<TaggedWord> segmentTag){
//        int indexVerb = getPositionContains(segmentTag, "/VB");
        int indexVerb = PosMethodsCondition.getVerbInCond(segmentTag);
        String mainVerb = "";
        if(indexVerb!=-1){
            mainVerb = segmentTag.get(indexVerb).word();
        }

        int indexKeyword = -1;
        int countWords = 0;
        String patternSub = "[a-z]{2}";
        if(indexVerb!=-1 && (indexVerb+1<segmentTag.size())){
            for(int i=indexVerb+1; i< segmentTag.size(); i++){
                if(MatchRegExpression(segmentTag.get(i).tag().toLowerCase(),patternSub)){
                    if(mainVerb.toLowerCase().trim().contains("are")||mainVerb.toLowerCase().trim().contains("is")||
                            mainVerb.toLowerCase().trim().contains("be") || mainVerb.toLowerCase().trim().contains("was")||
                            mainVerb.toLowerCase().trim().contains("were")){
                        countWords++;

                    }else if( !segmentTag.get(i).tag().toLowerCase().contains("vb")){
                        countWords++;
                    }
                }
            }
        }
        return countWords;
    }

    //Return number of words after a symbol that denotes action (e.g., =)
    public static Integer getNumberWordAftSymbol(ArrayList<TaggedWord> segmentTag){
        //indexCompareChar
        int countWords = 0;

        String patternSub = "[a-z]{2}";
        int indexCompareChar = PosMethods.firstSymbolCond(segmentTag);
        int indexKeyword = -1;
        if(indexCompareChar!=-1){
            for(int i=indexCompareChar; i< segmentTag.size(); i++){
                if(i+1 < segmentTag.size()){

                    if(MatchRegExpression(segmentTag.get(i+1).tag().toLowerCase(),patternSub)){

                        countWords++;

                    }
                }

            }
        }
        return countWords;
    }

    //returns the position of a word before the modal verb
    public static Integer getPosWordBefModVerb(ArrayList<TaggedWord> segmentTag, String keyword){
        int indexModVerb = getPositionContains(segmentTag, "/MD");
        int indexWord = -1;
        if(indexModVerb!=-1){
            for(int i=0; i< indexModVerb; i++){
                if(segmentTag.get(i).toString().toLowerCase().contains(keyword.toLowerCase())){
                    indexWord = i;
                    break;
                }

            }

        }
        return indexWord;
    }

    //returns the position of a word after the modal verb
    public static Integer getPosWordAftModVerb(ArrayList<TaggedWord> segmentTag, String keyword){
        //String[] wordSegments = segmentTag.split(" ");
        int indexModVerb = getPositionContains(segmentTag, "/MD");
        int indexKeyword = -1;
        if(indexModVerb!=-1){
            for(int i=indexModVerb; i< segmentTag.size(); i++){
                if(segmentTag.get(i).toString().toLowerCase().contains(keyword.toLowerCase())){
                    indexKeyword = i;
                    break;
                }
            }
        }
        return indexKeyword;
    }

    //Method that checks if a system response is not correct (e.g., incomplete)
    public static String isSegSRespIncorrect(ArrayList<TaggedWord> segmentTag){
        String typeSResp = "Other";
        int posSubConjA = getPositionContains(segmentTag, "/WRB");
        int posSubConjB = getPositionContains(segmentTag,"/WHADVP");
        int posSubConjC = getPositionContains(segmentTag,"if/IN");
        int posPrepSR = getPositionContains(segmentTag,"the/DT");
        int posNoun = getPositionContains(segmentTag,"/N");
        int posNounBefVerb = getPosWordBefModVerb(segmentTag,"/N");
        int posVB = getPositionContains(segmentTag, "/VB");
        int posVBAftModVerb = getPosWordAftModVerb(segmentTag, "/VB");
        int posFor = getPositionContains(segmentTag,"for/IN");
        int posModVerb = getPositionContains(segmentTag, "/MD");
        int[] posSubConjs= {posSubConjA,posSubConjB,posSubConjC};
        int posFirstSubConj = PosMethods.getShortNumber(posSubConjs);


        //it should contain modal verb?
        String singleSR = "NotSResponse";


        if(posNounBefVerb!=-1 && posModVerb!=-1 && posVBAftModVerb==-1){//Contains noun, modal, and NOT verb

            if(posNounBefVerb < posModVerb){
                singleSR = "SysRespIncomplNoVerb";
            }
        }else if(posNoun!=-1 && posModVerb==-1 && posVB!=-1){ //Contains noun, NOT modal, and verb

            if(posNoun < posVB){
                singleSR = "SysRespIncomplNoModal";
            }
        }else if(posNoun!=-1 && posModVerb==-1 && posVB==-1){ //Contains noun, NOT modal, and NOT verb
            //[else/RB, the/DT, reported/VBN, value/NN, must/MD, be/VB, MANU/NNP] Not working for this scenario

            singleSR = "SysRespIncomplNoModalNoVerb";
        }else if(posNounBefVerb==-1 && posModVerb!=-1 && posVBAftModVerb !=-1 ){//Contains  No noun , modal and verb

            if(posModVerb < posVBAftModVerb){
                singleSR = "SysRespIncomplNoNoun";
            }

        }else if(posNounBefVerb==-1 && posModVerb==-1 && posVBAftModVerb !=-1){
            singleSR = "SysRespIncomplNoNounNoModal";

        }else if(posNounBefVerb!=-1 && posModVerb!=-1 && posVBAftModVerb!=-1){ //Contains noun, modal, and  verb

            if(posNounBefVerb < posModVerb && posModVerb < posVBAftModVerb){
                singleSR = "SysRespCompl";
            }
        }
        if(posNounBefVerb!=-1 && posModVerb!=-1 && posFirstSubConj!=-1 && posFor==-1){//Contains Noun, modal verb, when and  NOT for
            if(posNounBefVerb < posFirstSubConj || posModVerb < posFirstSubConj){
                if(singleSR.contains("SysRespIncompl")){
                    typeSResp = singleSR;
                }else if(singleSR.equals("SysRespCompl")){
                    typeSResp = "SysRespIncorrect";
                }
            }
        }else if(posNounBefVerb!=-1 && posModVerb!=-1 && posFirstSubConj==-1 && posFor!=-1){//Contains Noun, modal verb, NOT when and  for
            if(posNounBefVerb < posFor || posModVerb < posFor){
                if(singleSR.contains("SysRespIncompl")){
                    typeSResp = singleSR;
                }else if(singleSR.equals("SysRespCompl")){
                    typeSResp = "SysRespIncorrect";
                }
            }
        }else if(posNounBefVerb!=-1 && posModVerb!=-1 && posFirstSubConj==-1 && posFor==-1){//Contains Noun, modal verb, NOT when and  NOT for
            if(singleSR.contains("SysRespIncompl")){
                typeSResp = singleSR;
            }
        }else if(posModVerb!=-1 && posFirstSubConj!=-1 && posFor==-1){//Contains  modal verb,  when and  NOT for
            if(posModVerb < posFirstSubConj){
                if(singleSR.contains("SysRespIncompl")){
                    typeSResp = singleSR;
                }else if(singleSR.equals("SysRespCompl")){
                    typeSResp = "SysRespIncorrect";
                }
            }
        }else if(posModVerb!=-1 && posFirstSubConj==-1 && posFor!=-1){//Contains  modal verb, NOT when and   for
            if(posModVerb< posFor){
                if(singleSR.contains("SysRespIncompl")){
                    typeSResp = singleSR;
                }else if(singleSR.equals("SysRespCompl")){
                    typeSResp = "SysRespIncorrect";
                }
            }
        }else if(posModVerb!=-1 && posFirstSubConj==-1 && posFor==-1){//Contains  modal verb, NOT when and  NOT for
            if(singleSR.contains("SysRespIncompl")){
                typeSResp = singleSR;
            }
        }
        return typeSResp;
    }

    //returns the verb of a system response
    public static String getVerbSegmentSR(ArrayList<TaggedWord> segmentTagged){
        String verb = "";
        int modVerb = PosMethods.getPositionContains(segmentTagged,"MD");
        int posVerb = -1;
        if(modVerb!=-1){
            posVerb = PosMethods.getPosWordFromSpecLoc(segmentTagged,"VB", modVerb);
        }

        if(posVerb!=-1){
            verb = PosMethods.getElementSpecLoc(segmentTagged,"VB", modVerb);
        }
        return verb;
    }


    //returns true is the segment is a system response
    public static boolean isSegmentSR(ArrayList<TaggedWord> segmentTagged, boolean unique){
        boolean isSR = false;
//        int indexFirstKeyword = getPositionContains(segmentTagged,"/N");
        int indexFirstKeyword = getPositionNounInSr(segmentTagged);

        int indexSecondKeyword = getPositionContains(segmentTagged, "/MD");

        if(indexFirstKeyword == -1 || (indexFirstKeyword > indexSecondKeyword)){
            indexFirstKeyword = getPositionContains(segmentTagged,"it/PRP");
        }

        int indexThirdKeyword = getPosWordAftModVerb(segmentTagged, "/VB");

        if(!hasSubConj(segmentTagged)){

            if(indexFirstKeyword!=-1 && indexSecondKeyword!=-1 && indexThirdKeyword!=-1){
                if((indexFirstKeyword < indexSecondKeyword) && (indexSecondKeyword<indexThirdKeyword)){
                    if(unique){
                        if(getFreqWord(segmentTagged,"/MD") == 1){
                            isSR = true;
                        }else{
                            isSR = false;
                        }
                    }else{
                        isSR =true;
                    }
                }
            }


        }


        return isSR;
    }



    //get position of nouns in a system response
    public static int getPositionNounInSr(ArrayList<TaggedWord> segmentTag){
        ArrayList <String> prepositions = new ArrayList<>();
        prepositions.add("i");
        prepositions.add("you");
        prepositions.add("it");
        prepositions.add("they");
        prepositions.add("he");
        prepositions.add("she");
        prepositions.add("we");

        ArrayList<String> keywords = new ArrayList<>();
        keywords.add("/N");
        keywords.add("/PRP");

        int indexKeyword = -1;
        outerloop:
        for(int i=0; i< segmentTag.size(); i++){
            if(!segmentTag.get(i).word().contains("\\*")){

                for(String key:keywords){
                    if(segmentTag.get(i).toString().toLowerCase().contains(key.toLowerCase())){
                        if(key.equals("/N")){

                            indexKeyword = i;
                            break outerloop;
                        }else{ //Preposition
                            for(String prep:prepositions){

                                if(segmentTag.get(i).word().trim().toLowerCase().equals(prep.toLowerCase())){

                                    indexKeyword = i;
                                    break outerloop;
                                }
                            }
                        }
                    }

                }
            }
        }
        return indexKeyword;
    }

    //get positions of the separators within a condition
    public static ArrayList<Integer> getIndexSepCond(ArrayList<TaggedWord> s){
        ArrayList<Integer> positionSep = new ArrayList<>();
        String patternSub = "\\b(when|where|while|if|until|and|or)\\b";

        for(int i=0; i< s.size(); i++){

            if(s.get(i).tag().equals("CC")||s.get(i).tag().equals("WRB")||
                    s.get(i).tag().equals("WHADVP")||s.get(i).tag().equals("IN")){
                if(MatchRegExpression(s.get(i).word().toLowerCase().trim(), patternSub))
                {
                    if(s.get(i).word().toLowerCase().trim().equals("until")){
                        //the method hasConditionUntil checks from the i+1 that should have until
                        if(hasConditionUntil(i-1,s)){
                            positionSep.add(i);
                        }

                    }else{

                        positionSep.add(i);
                    }
                }
            }else if(i==0 || i==(s.size()-1)){
                positionSep.add(i);

            }
        }
        return positionSep;
    }

    //get positions of the separators within a system response
    public static ArrayList<Integer>  getIndexSepSResp(ArrayList<TaggedWord> s){
        ArrayList<Integer> positionSep = new ArrayList<>();
        String patternSub = "\\b(otherwise|else|then|and|or)\\b";
        for(int i=0; i< s.size(); i++){
            if(MatchRegExpression(s.get(i).word().toLowerCase().trim(), patternSub)){
                positionSep.add(i);
            }else if(i==0 || i==(s.size()-1)){
                positionSep.add(i);
            }
        }
        return positionSep;

    }

    //Method that returns the positions of the verbs and symbols that denote actions on a sentence
    public static ArrayList<Integer> getIndexVerbsAndSymbolSentence(ArrayList<TaggedWord> sentence){
        ArrayList<Integer> positionVerbs = new ArrayList<>();
        String patternSub = "(=|<|>|\\u2260)";
        for(int i=0; i< sentence.size(); i++){
            if(sentence.get(i).toString().contains("VB") || MatchRegExpression(sentence.get(i).toString(),patternSub)){
                positionVerbs.add(i);
            }
        }
        return positionVerbs;
    }

    //Method that returns the positions of the verbs given a sentence
    public static ArrayList<Integer> getIndexVerbsSentence(ArrayList<TaggedWord> sentence){
        ArrayList<Integer> positionVerbs = new ArrayList<>();
        //Assume that the first verb then we have "and"
        for(int i=0; i< sentence.size(); i++){
            if(sentence.get(i).toString().contains("VB")){
                positionVerbs.add(i);
            }
        }
        return positionVerbs;
    }

    // Method that tries to identify different actions in a SR

    public static boolean hasSegActions(ArrayList<TaggedWord> seg){
        boolean multActions = false;
        int short_dist = -1;
        int long_dist = -1;
        boolean hasconnector = false;
        ArrayList<Integer> positionVerbs = getIndexVerbsSentence(seg);
        String regexConnectors = "\\b(otherwise|else|then|and|or|and\\/or)\\b";

        if(positionVerbs.size() > 1){
            for (int i=0; i<positionVerbs.size(); i++){
                if( (i+1) < positionVerbs.size()){ //Check not in the last position
                    int distance =  positionVerbs.get(i) - positionVerbs.get(i+1);

                    if(distance == -1){
                        short_dist++;

                    }else if(distance == -2){

                        for(int j=positionVerbs.get(i); j < positionVerbs.get(i+1) ;j++){
                            if(MatchRegExpression(seg.get(j).word().toLowerCase().trim(),regexConnectors)){
                                long_dist ++;
                                hasconnector = true;
                            }
                        }
                        if(!hasconnector){
                            short_dist++;
                        }
                    }else if(distance <= -3){
                        long_dist ++;
                    }
                }
            }
            if(long_dist != -1){
                multActions = true;
            }
        }
        return  multActions;
    }

    //Return true if the segement/clause has an action or a symbol (="equal to")
    public static boolean hasSegActionsAndSymbol(ArrayList<TaggedWord> seg){
        boolean multActions = false;
        int short_dist = -1;
        int long_dist = -1;
        boolean hasconnector = false;
        //Positions of the verbs and symbols
        ArrayList<Integer> positionVerbs = getIndexVerbsAndSymbolSentence(seg);


        if(positionVerbs.size() > 1){
            for (int i=0; i<positionVerbs.size();i++ ){
                if( (i+1) < positionVerbs.size()){ //Check not in the last position
                    int distance =  positionVerbs.get(i) - positionVerbs.get(i+1);

                    if(distance == -1){
                        short_dist++;

                    }else if(distance == -2){

                        for(int j=positionVerbs.get(i); j < positionVerbs.get(i+1) ;j++){


                            if(seg.get(j).word().toLowerCase().trim().equals("and") || seg.get(j).word().toLowerCase().trim().equals("or") ||
                                    seg.get(j).word().toLowerCase().trim().equals("and/or") || seg.get(j).word().toLowerCase().trim().equals("or/and") ){
                                long_dist ++;
                                hasconnector = true;
                            }
                        }
                        if(!hasconnector){
                            short_dist++;
                        }
                    }else if(distance <= -3){
                        for(int j=positionVerbs.get(i); j < positionVerbs.get(i+1) ;j++){
                            if(seg.get(j).word().toLowerCase().trim().equals("and") || seg.get(j).word().toLowerCase().trim().equals("or") ||
                                    seg.get(j).word().toLowerCase().trim().equals("and/or") || seg.get(j).word().toLowerCase().trim().equals("or/and") ){
                                long_dist ++;
                                hasconnector = true;
                            }
                        }
                        if(!hasconnector){
                            short_dist++;
                        }
                        long_dist ++;
                    }
                }
            }
            if(long_dist != -1){
                multActions = true;
            }
        }
        return  multActions;
    }





    //Retrieve the separated actions from SR  separated by "and" or "or"
    public  static ArrayList<Sresp> getSingleActionsSR(ArrayList<TaggedWord> seg){
        ArrayList<Sresp> segsInSresp = new ArrayList<>();
        ArrayList<Integer> positionSeps = PosMethods.getIndexSepSResp(seg);

        for (int i=0; i< positionSeps.size(); i++ ){
            if( (i+1) < positionSeps.size()){
                if(i != (positionSeps.size()-2)){

                    int distance =  positionSeps.get(i) - positionSeps.get(i+1);
                    if(distance <= -3){
                        Sresp next = typeSrBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), (positionSeps.get(i+1) -1)));
                        segsInSresp.add(next);
                    }else{
                        Sresp next = typeSrBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), (positionSeps.get(i+1) -1)));
                        segsInSresp.add(next);
                    }
                }else{
                    int distance =  positionSeps.get(i) - positionSeps.get(i+1);
                    if(distance <= -3){
                        Sresp next = typeSrBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), positionSeps.get(i+1)));
                        segsInSresp.add(next);
                    }else{
                        Sresp next = typeSrBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), positionSeps.get(i+1)));
                        segsInSresp.add(next);
                    }
                }
            }
        }

        ArrayList<Sresp> mergedSresps = mergeSrespSegments(segsInSresp);
        return mergedSresps;
    }

    //given two positions it returns a part of the segment/clause
    public static ArrayList<TaggedWord> getSegmentFromList(ArrayList<TaggedWord> seg, int start, int end){
        ArrayList<TaggedWord> singleAct = new ArrayList<>();
        for( int i = start; i<= end; i++){
            singleAct.add(seg.get(i));
        }
        return singleAct;
    }

    //merges two system responses into one
    public static ArrayList<TaggedWord> mergeSysResp(ArrayList<TaggedWord> segA, ArrayList<TaggedWord> segB){
        ArrayList<TaggedWord> merged = new ArrayList<>();

        for(TaggedWord w: segA){
            merged.add(w);
        }
        for(TaggedWord w:segB){
            merged.add(w);
        }
        return merged;
    }

    //returns last word of the system response
    public static ArrayList<TaggedWord> getLastWordSresp(ArrayList<Sresp> listCond){
        ArrayList<TaggedWord> lastElement = new ArrayList<>();
        if(listCond.size()>0){
            lastElement = listCond.get(listCond.size()-1).getsRespTagged();
        }
        return lastElement;
    }

    //method that returns true if there are words after the verb of the system response
    public static boolean hasWordAfterVerbSresp(ArrayList<TaggedWord> seg){
        boolean hasNoun = false;
        int indexVB = PosMethods.getPositionContains(seg, "/VB");

        int wordsAftVerb = PosMethods.getNumberWordAfterVerb(seg);
        int wordsAfterSymbol = PosMethods.getNumberWordAftSymbol(seg);
//        int indexNounAft = PosMethods.getPosWordAftVerb(seg,"/N");
//        int indexNounAftCompChar = PosMethods.getPosWordAftCompChar(seg,"/N");
        int indexCompareChar = PosMethods.firstSymbolCond(seg);

        if((indexVB!=-1 && wordsAftVerb>0) || (indexCompareChar!=-1 && wordsAfterSymbol > 0)){
            hasNoun = true;
        }
        return hasNoun;
    }

    //method that merges segments/clauses
    public static ArrayList<Sresp> mergeSrespSegments(ArrayList<Sresp> segsInSresp){
        ArrayList<Sresp> mergedSysResps = new ArrayList<>();
        boolean beforeWasMerged = false;

        for(int i=0; i< segsInSresp.size();i++){
            if(i!=0){
                if(segsInSresp.get(i).getNotMatched() || segsInSresp.get(i).getsRespTagged().size() < 3 ){
                    if(!beforeWasMerged){
                        ArrayList<TaggedWord> mergedSeg = mergeSysResp(segsInSresp.get(i-1).getsRespTagged(),segsInSresp.get(i).getsRespTagged());
                        Sresp mergedSRes = typeSrBullet(mergedSeg);
                        if(PosMethods.getTextFromSentence(getLastWordSresp(mergedSysResps)).equals(PosMethods.getTextFromSentence(segsInSresp.get(i-1).getsRespTagged()))){
                            //then we need to remove last element from the
                            if(mergedSysResps.size() > 0){
                                mergedSysResps.remove(mergedSysResps.size()-1);
                            }
                        }
                        beforeWasMerged = true;
                        mergedSysResps.add(mergedSRes);
                    }
                }else if(!hasWordAfterVerbSresp(segsInSresp.get(i).getsRespTagged())){
                    if(!beforeWasMerged){
                        ArrayList<TaggedWord> mergedSeg = mergeSysResp(segsInSresp.get(i-1).getsRespTagged(),segsInSresp.get(i).getsRespTagged());
                        Sresp mergedSRes = typeSrBullet(mergedSeg);
                        if(PosMethods.getTextFromSentence(getLastWordSresp(mergedSysResps)).equals(PosMethods.getTextFromSentence(segsInSresp.get(i-1).getsRespTagged()))){
                            //then we need to remove last element from the
                            if(mergedSysResps.size() > 0){
                                mergedSysResps.remove(mergedSysResps.size()-1);
                            }
                        }
                        beforeWasMerged = true;
                        mergedSysResps.add(mergedSRes);
                    }else{
                        mergedSysResps.add(segsInSresp.get(i));

                    }
                }else {
                    if(i==1){
                        beforeWasMerged = false;
                        mergedSysResps.add(segsInSresp.get(i-1)); //and
                        mergedSysResps.add(segsInSresp.get(i));
                    }else{
                        beforeWasMerged = false;
                        mergedSysResps.add(segsInSresp.get(i));

                    }
                }

            }
        }
        return mergedSysResps;
    }

    //returns the concepts in the system response
    public static List<TupleConcept> conceptsInSR(ArrayList<TaggedWord> segmentTagged){
        List<TupleConcept> listConcepts = new ArrayList<>();
        int indexNoun = getPositionContains(segmentTagged,"/N");
        int indexMV = getPositionContains(segmentTagged, "/MD");
        int indexVB = getPositionContains(segmentTagged, "/VB");
        TupleConcept actor = new TupleConcept("actor",segmentTagged.get(indexNoun).word());
        TupleConcept modal = new TupleConcept("modal", segmentTagged.get(indexMV).word());
//        TupleConcept verb = new TupleConcept("verb",segmentTagged.get(indexVB).word());
        listConcepts.add(actor);
        listConcepts.add(modal);
//        listConcepts.add(verb);
        return listConcepts;
    }

    //Checks if there is a passive voice in the SR
    public static boolean isPassiveVoiceSr(ArrayList<TaggedWord> segments) {
        boolean isPVSr = false;
        int indexModVerb = firstModalVerb(segments);
        int idFirstVb = -1;
        if (indexModVerb != -1) {
            idFirstVb = getPosWordFromSpecLoc(segments, "/VB", indexModVerb);
        }

        int countSubseqVerbs = 0;
        //There is a modal verb and a verb
        if (idFirstVb != -1) {
            for (int j = indexModVerb + 1; j < segments.size(); j++) {
                if (segments.get(j).tag().contains("VB") ||
                        segments.get(j).toString().contains("not/RB")) {
                    countSubseqVerbs += 1;
                } else {
                    break;
                }
            }
        }

        int segmentSize = indexModVerb + countSubseqVerbs;

        if(idFirstVb != -1){
            if (indexModVerb + 3 <= segmentSize) {//3 words after auxiliar verb
                String negText = segments.get(indexModVerb+1).word();
                String negPos = segments.get(indexModVerb+1).tag();

                String firstVbText = segments.get(indexModVerb + 2).word();
                String firstVbPos = segments.get(indexModVerb + 2).tag();

                String secVbText = segments.get(indexModVerb + 3).word();
                String secVbPos = segments.get(indexModVerb + 3).tag();



                //If it has negation
                if(negPos.contains("RB") && negText.toLowerCase().contains("not")){
                    if(firstVbPos.contains("VB") && secVbPos.contains("VB")){
                        if(firstVbText.toLowerCase().equals("be") || firstVbText.toLowerCase().equals("have")){
                            isPVSr = true;
                        }
                    }
                }
            }else  if (indexModVerb + 2 <= segmentSize) {
                // must be   played
                // must have played
                String firstVbText = segments.get(indexModVerb+1).word();
                String firstVbPos = segments.get(indexModVerb+1).tag();
                String secVbText = segments.get(indexModVerb + 2).word();
                String secVbPos = segments.get(indexModVerb + 2).tag();


                if((firstVbPos.contains("VB") && firstVbText.toLowerCase().contains("be")) ||
                        (firstVbPos.contains("VB") && firstVbText.toLowerCase().contains("have"))){
                    if((secVbPos.equals("VBN") || secVbPos.equals("VBD"))){

                        isPVSr = true;

                    }
                }
            }


        }

        return isPVSr;
    }

    //get the verb of a passive voice in SR
    public static String getVerbPassiveVoiceSr(ArrayList<TaggedWord> segments){
        String mainVerb = "";
        int indexModVerb = firstModalVerb(segments);
        int idFirstVb = -1;
        if (indexModVerb != -1) {
            idFirstVb = getPosWordFromSpecLoc(segments, "/VB", indexModVerb);
        }
        int countSubseqVerbs = 0;
        //There is a modal verb and a verb
        if (idFirstVb != -1) {
            for (int j = indexModVerb + 1; j < segments.size(); j++) {
                if (segments.get(j).tag().contains("VB") ||
                        segments.get(j).toString().contains("not/RB")) {
                    countSubseqVerbs += 1;
                } else {
                    break;
                }
            }
        }
        int segmentSize = indexModVerb + countSubseqVerbs;
        if(idFirstVb != -1){
            if (indexModVerb + 3 <= segmentSize) {//3 words after auxiliar verb
                String negText = segments.get(indexModVerb+1).word();
                String negPos = segments.get(indexModVerb+1).tag();

                String firstVbText = segments.get(indexModVerb + 2).word();
                String firstVbPos = segments.get(indexModVerb + 2).tag();

                String secVbText = segments.get(indexModVerb + 3).word();
                String secVbPos = segments.get(indexModVerb + 3).tag();
                //If it has negation
                if(negPos.contains("RB") && negText.toLowerCase().contains("not")){
                    if(firstVbPos.contains("VB") && secVbPos.contains("VB")){
                        if(firstVbText.toLowerCase().equals("be") || firstVbText.toLowerCase().equals("have")){
                            mainVerb = secVbText;
                        }
                    }
                }
            }else  if (indexModVerb + 2 <= segmentSize) {
                // must be   played
                // must have played
                String firstVbText = segments.get(indexModVerb+1).word();
                String firstVbPos = segments.get(indexModVerb+1).tag();
                String secVbText = segments.get(indexModVerb + 2).word();
                String secVbPos = segments.get(indexModVerb + 2).tag();


                if((firstVbPos.contains("VB") && firstVbText.toLowerCase().contains("be")) ||
                        (firstVbPos.contains("VB") && firstVbText.toLowerCase().contains("have"))){
                    if((secVbPos.equals("VBN") || secVbPos.equals("VBD"))){

                        mainVerb = secVbText;

                    }
                }
            }
        }
        return  mainVerb;
    }

    //Method that retrieves tha checks if there is a passive voice in condition
    public static boolean isPassiveVoice(ArrayList<TaggedWord> segmentTagged) {
        boolean isPVCond = false;
        boolean hasConseqVerb = false;
        //we know that it is a condition but the condition can have different verbs

        int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
        int idFirstVb = -1;
        if(nounCond!=-1){
            idFirstVb = getPosWordFromSpecLoc(segmentTagged, "/VB",nounCond );
        }
        //Find the first VB and then checks the following words which must be verbs
        int countSubseqVerbs = 0;
        if (idFirstVb != -1){
            for (int j=idFirstVb+1; j < segmentTagged.size(); j++){

                if(segmentTagged.get(j).tag().contains("VB") ||
                        segmentTagged.get(j).toString().contains("not/RB")){
                    countSubseqVerbs += 1;
                }else{
                    break; //Control subsequents
                }
            }
        }

        int verbPastPosition = -1;
        int posFirstVerbPVoice = -1;

        if(countSubseqVerbs == 0){
            if(nounCond!=-1){
                verbPastPosition = getPosVerbFromSpecLoc(segmentTagged, "VBN", nounCond);
            }
            if (verbPastPosition != -1){
                posFirstVerbPVoice = getPosWordFromSpecLoc(segmentTagged, "/VB",verbPastPosition + 1);
            }

            if (posFirstVerbPVoice!= -1){
                for (int k=posFirstVerbPVoice+1; k < segmentTagged.size(); k++){

                    if(segmentTagged.get(k).tag().contains("VB") ||
                            segmentTagged.get(k).toString().contains("not/RB")){
                        countSubseqVerbs += 1;
                    }else{
                        break; //Control subsequents
                    }
                }

            }
            if (countSubseqVerbs > 0){
                idFirstVb = posFirstVerbPVoice;
            }
        }

        int segmentSize = idFirstVb + countSubseqVerbs;
        //passive voice could be composed of 2 or 3 words
        if(idFirstVb!= -1 && (countSubseqVerbs > 0))  {

            if (idFirstVb + 3 <= segmentSize) { //3 words after auxiliar verb
                String firstVbText = segmentTagged.get(idFirstVb).word();
                String firstVbPos = segmentTagged.get(idFirstVb).tag();
                String secVbText = segmentTagged.get(idFirstVb + 1).word();
                String secVbPos = segmentTagged.get(idFirstVb + 1).tag();
                String thirdVbText = segmentTagged.get(idFirstVb + 2).word();
                String thirdVbPos = segmentTagged.get(idFirstVb + 2).tag();

                String negText = segmentTagged.get(idFirstVb + 1).word();
                String negPos = segmentTagged.get(idFirstVb + 1).tag();
                String secVbTextNeg = segmentTagged.get(idFirstVb + 2).word();
                String secVbPosNeg = segmentTagged.get(idFirstVb + 2).tag();
                String thirdVbTextNeg = segmentTagged.get(idFirstVb + 3).word();
                String thirdVbPosNeg = segmentTagged.get(idFirstVb + 3).tag();

                //If it has negation
                if(negPos.contains("RB") && negText.toLowerCase().contains("not")){
                    if(firstVbPos.contains("VB") && secVbPosNeg.contains("VB") &&
                            (thirdVbPosNeg.equals("VBN")||thirdVbPosNeg.equals("VBD"))){
                        if(firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have")){
                            if (secVbTextNeg.toLowerCase().equals("been")) {
                                isPVCond = true;
                            }
                        }
                    }else if(firstVbPos.contains("VB") && (secVbPosNeg.equals("VBN") || secVbPosNeg.equals("VBD")) && !thirdVbPosNeg.contains("VB")){
                        // (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))
                        if((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are")) ){
                            isPVCond= true;

                        }

                    }
                }else{
                    if (firstVbPos.contains("VB") && secVbPos.contains("VB") &&
                            (thirdVbPos.equals("VBN") || thirdVbPos.equals("VBD"))) {
                        if (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have")) {
                            if (secVbText.toLowerCase().equals("been")) {
                                isPVCond = true;
                            }
                        }
                    } else if (firstVbPos.contains("VB") && (secVbPos.equals("VBN") || secVbPos.equals("VBD")) && !thirdVbPos.contains("VB")) {
                        //                                (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))

                        if ((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are"))) {
                            isPVCond = true;
                        }
                    }
                }
            }else if (idFirstVb + 2 <= segmentSize) { //2 words after auxiliar verb

                String firstVbText = segmentTagged.get(idFirstVb).word();
                String firstVbPos = segmentTagged.get(idFirstVb).tag();
                String secVbText = segmentTagged.get(idFirstVb + 1).word();
                String secVbPos = segmentTagged.get(idFirstVb + 1).tag();
                String thirdVbText = segmentTagged.get(idFirstVb + 2).word();
                String thirdVbPos = segmentTagged.get(idFirstVb + 2).tag();
                //If it has negation
                String negText = segmentTagged.get(idFirstVb + 1).word();
                String negPos = segmentTagged.get(idFirstVb + 1).tag();
                String secVbTextNeg = segmentTagged.get(idFirstVb + 2).word();
                String secVbPosNeg = segmentTagged.get(idFirstVb + 2).tag();
                //(firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))

                if(negPos.contains("RB") && (negText.toLowerCase().contains("not") || negText.toLowerCase().contains("success") )){
                    if(firstVbPos.contains("VB") && (secVbPosNeg.equals("VBN") || secVbPosNeg.equals("VBD"))){
                        if(firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are")) {
                            isPVCond = true;
                        }
                    }
                }else{
                    if (firstVbPos.contains("VB") && secVbPos.contains("VB") &&
                            (thirdVbPos.equals("VBN") || thirdVbPos.equals("VBD"))) {
                        if (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have")) {
                            if (secVbText.toLowerCase().equals("been")) {
                                isPVCond = true;
                            }
                        }
                    } else if (firstVbPos.contains("VB") && (secVbPos.equals("VBN") || secVbPos.equals("VBD")) && !thirdVbPos.contains("VB")) {
                        //|| (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))
                        if ((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are"))) {
                            isPVCond = true;
                        }
                    }
                }

            } else if (idFirstVb + 1 <= segmentSize) { //1 word after auxiliar verb


                String firstVbText = segmentTagged.get(idFirstVb).word();
                String firstVbPos = segmentTagged.get(idFirstVb).tag();
                String secVbText = segmentTagged.get(idFirstVb + 1).word();
                String secVbPos = segmentTagged.get(idFirstVb + 1).tag();

                //(firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))
                if (firstVbPos.contains("VB") && (secVbPos.equals("VBN") || secVbPos.equals("VBD"))) {
                    if (firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are") || firstVbText.toLowerCase().equals("be") ) {
                        isPVCond = true;
                    }
                }
            }
        }

        return isPVCond;
    }

    //Method that retrieves the verb of the passive voice in condition
    public static String getVerbPassiveVoice(ArrayList<TaggedWord> segmentTagged){
        String verbPassive = "";
        boolean isPVCond = false;
        boolean hasConseqVerb = false;
        //we know that it is a condition

        int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
        int idFirstVb = -1;
        if(nounCond!=-1){
            //Reinvest
            idFirstVb = getPosWordFromSpecLoc(segmentTagged, "/VB",nounCond );
        }

        int countSubseqVerbs = 0;


        if (idFirstVb != -1){
            for (int j=idFirstVb+1; j < segmentTagged.size(); j++){
                if(segmentTagged.get(j).tag().contains("VB") || segmentTagged.get(j).word().contains("not") || segmentTagged.get(j).word().contains("success")){
                    countSubseqVerbs += 1;
                }else{
                    break;
                }
            }
        }

        int verbPastPosition = -1;
        int posFirstVerbPVoice = -1;

        if(countSubseqVerbs == 0){
            if(nounCond!=-1){
                verbPastPosition = getPosVerbFromSpecLoc(segmentTagged, "VBN", nounCond);
            }
            if (verbPastPosition != -1){
                posFirstVerbPVoice = getPosWordFromSpecLoc(segmentTagged, "/VB",verbPastPosition + 1);
            }


            if (posFirstVerbPVoice!= -1){
                for (int k=posFirstVerbPVoice+1; k < segmentTagged.size(); k++){

                    if(segmentTagged.get(k).tag().contains("VB") ||
                            segmentTagged.get(k).toString().contains("not/RB")){
                        countSubseqVerbs += 1;
                    }else{
                        break; //Control subsequents
                    }
                }

            }
            if (countSubseqVerbs > 0){
                idFirstVb = posFirstVerbPVoice;
            }
        }

        int segmentSize = idFirstVb + countSubseqVerbs;
        //passive voice coould be composed of 2 or 3 words
        if(idFirstVb!= -1) {

            if (idFirstVb + 3 <= segmentSize) { //3 words after auxiliar verb
                String firstVbText = segmentTagged.get(idFirstVb).word();
                String firstVbPos = segmentTagged.get(idFirstVb).tag();
                String secVbText = segmentTagged.get(idFirstVb + 1).word();
                String secVbPos = segmentTagged.get(idFirstVb + 1).tag();
                String thirdVbText = segmentTagged.get(idFirstVb + 2).word();
                String thirdVbPos = segmentTagged.get(idFirstVb + 2).tag();

                String negText = segmentTagged.get(idFirstVb + 1).word();
                String negPos = segmentTagged.get(idFirstVb + 1).tag();
                String secVbTextNeg = segmentTagged.get(idFirstVb + 2).word();
                String secVbPosNeg = segmentTagged.get(idFirstVb + 2).tag();
                String thirdVbTextNeg = segmentTagged.get(idFirstVb + 3).word();
                String thirdVbPosNeg = segmentTagged.get(idFirstVb + 3).tag();

                //If it has negation
                if(negPos.contains("RB") && negText.toLowerCase().contains("not")){
                    if(firstVbPos.contains("VB") && secVbPosNeg.contains("VB") &&
                            (thirdVbPosNeg.equals("VBN")||thirdVbPosNeg.equals("VBD"))){
                        if(firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have")){
                            if (secVbTextNeg.toLowerCase().equals("been")) {
                                verbPassive = thirdVbTextNeg;
                            }
                        }
                    }else if(firstVbPos.contains("VB") && (secVbPosNeg.equals("VBN") || secVbPosNeg.equals("VBD")) && !thirdVbPosNeg.contains("VB")){
                        //||
                        //                                (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))
                        if((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are"))  ){
                            verbPassive= thirdVbTextNeg;
                        }
                    }
                }else{
                    if (firstVbPos.contains("VB") && secVbPos.contains("VB") &&
                            (thirdVbPos.equals("VBN") || thirdVbPos.equals("VBD"))) {
                        if (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have")) {
                            if (secVbText.toLowerCase().equals("been")) {
                                verbPassive = thirdVbText;
                            }
                        }
                    } else if (firstVbPos.contains("VB") && (secVbPos.equals("VBN") || secVbPos.equals("VBD")) && !thirdVbPos.contains("VB")) {
                        //||
                        //                                (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))

                        if ((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are"))  ) {
                            verbPassive = thirdVbText;
                        }
                    }
                }
            }else if (idFirstVb + 2 <= segmentSize) { //2 words after auxiliar verb

                String firstVbText = segmentTagged.get(idFirstVb).word();
                String firstVbPos = segmentTagged.get(idFirstVb).tag();
                String secVbText = segmentTagged.get(idFirstVb + 1).word();
                String secVbPos = segmentTagged.get(idFirstVb + 1).tag();
                String thirdVbText = segmentTagged.get(idFirstVb + 2).word();
                String thirdVbPos = segmentTagged.get(idFirstVb + 2).tag();
                //If it has negation
                String negText = segmentTagged.get(idFirstVb + 1).word();
                String negPos = segmentTagged.get(idFirstVb + 1).tag();
                String secVbTextNeg = segmentTagged.get(idFirstVb + 2).word();
                String secVbPosNeg = segmentTagged.get(idFirstVb + 2).tag();
                //||
                // (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))
                if(negPos.contains("RB") && (negText.toLowerCase().contains("not") || negText.toLowerCase().contains("success") )){
                    if(firstVbPos.contains("VB") && (secVbPosNeg.equals("VBN") || secVbPosNeg.equals("VBD"))){
                        if((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are") ||
                                firstVbText.toLowerCase().equals("was") || firstVbText.toLowerCase().equals("were")
                        )  ) {
                            verbPassive = secVbTextNeg;
                        }
                    }
                }else{
                    if (firstVbPos.contains("VB") && secVbPos.contains("VB") &&
                            (thirdVbPos.equals("VBN") || thirdVbPos.equals("VBD"))) {
                        if (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have")) {
                            if (secVbText.toLowerCase().equals("been")) {
                                verbPassive = thirdVbText;
                            }
                        }
                    } else if (firstVbPos.contains("VB") && (secVbPos.equals("VBN") || secVbPos.equals("VBD")) && !thirdVbPos.contains("VB")) {
                        //||
                        //                                (firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))
                        if ((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are")
                        || firstVbText.toLowerCase().equals("was") || firstVbText.toLowerCase().equals("were") )  )  {
                            verbPassive = thirdVbText;
                        }
                    }
                }

            } else if (idFirstVb + 1 <= segmentSize) { //1 word after auxiliar verb
                String firstVbText = segmentTagged.get(idFirstVb).word();
                String firstVbPos = segmentTagged.get(idFirstVb).tag();
                String secVbText = segmentTagged.get(idFirstVb + 1).word();
                String secVbPos = segmentTagged.get(idFirstVb + 1).tag();
                if (firstVbPos.contains("VB") && (secVbPos.equals("VBN") || secVbPos.equals("VBD"))) {
                    //(firstVbText.toLowerCase().equals("has") || firstVbText.toLowerCase().equals("have"))
                    if ((firstVbText.toLowerCase().equals("is") || firstVbText.toLowerCase().equals("are")
                    || firstVbText.toLowerCase().equals("was") || firstVbText.toLowerCase().equals("were") ||
                            firstVbText.toLowerCase().equals("be"))) {
                        verbPassive = secVbText;
                    }
                }
            } else{ //when there is only a verb
                String firstVbText = segmentTagged.get(idFirstVb).word();
                verbPassive = firstVbText;
            }
        }
        return verbPassive;

    }

    //MEthod that returns the concepts of the condition type s8
    public static List<TupleConcept> conceptsInCondS8(ArrayList<TaggedWord> segmentTagged){
        List<TupleConcept> listConcepts = new ArrayList<>();
        int indexNoun = getPositionContains(segmentTagged,"/N");
        int indexVB = getPositionContains(segmentTagged, "/VB");
        TupleConcept actor = new TupleConcept("actor",segmentTagged.get(indexNoun).word());
        TupleConcept verb = new TupleConcept("verb",segmentTagged.get(indexVB).word());
        listConcepts.add(actor);
        listConcepts.add(verb);
        return listConcepts;
    }


    //MEthod that returns the concepts of the condition type s10
    public static List<TupleConcept> conceptsInS10(ArrayList<TaggedWord> segmentTagged){
        List<TupleConcept> conceptsS10 =  new ArrayList<>();
        int verbIncomp = getPositionContains(segmentTagged, "/N");
        String verbCondInc = segmentTagged.get(verbIncomp).word();
        TupleConcept verbCond10 = new TupleConcept("verbCondInc", verbCondInc);
        conceptsS10.add(verbCond10);
        return conceptsS10;
    }


    //MEthod that returns the concepts of the condition
    public static List<TupleConcept> conceptsS8Sub(ArrayList<TaggedWord> segsTag){
        List<TupleConcept> conceptsS8Sub = new ArrayList<>();
        int actorPos =  getPositionContains(segsTag, "/N");
        int verbPos =   getPositionContains(segsTag, "/VB");
        String actor = segsTag.get(actorPos).word();
        String verbCon = segsTag.get(verbPos).word();
        TupleConcept conActor = new TupleConcept("actor", actor) ;
        TupleConcept verbCond = new TupleConcept("verb", verbCon);
        conceptsS8Sub.add(conActor);
        conceptsS8Sub.add(verbCond);
        return conceptsS8Sub;
    }


    //Method that identifies segment/clauses
    public static SegmentReq getTypeOfSegment(SegmentReq s, MaxentTagger tagger, String cleanReq, String reqId, String typePrevSeg)
    {
        String type ="";
        String verbCondition = "";
        String seg = Utils.getStringFromSegment(s.getStartSegment(),s.getEndSegment(),cleanReq);
        ArrayList<TaggedWord> segmentTagged = getTagSentence(seg,tagger);
        int compReq = 0;// has more than one segment

        if(isSegmentScope(segmentTagged)){
            type += " scopePos";
        }else if(PosMethods.isSegScopeIncorrect(segmentTagged).equals("ScopeIncorr") || PosMethods.isSegScopeIncorrect(segmentTagged).equals("ScopeIncompl")){
            type += " "+isSegScopeIncorrect(segmentTagged); //ScopePosIncomplete";

        }else if(isSegmentSR(segmentTagged, false)){
            ArrayList<Sresp> sysResps = PosMethods.detectMultSysResponses(segmentTagged);
            String typeSresps = PosMethods.getTypeMultSresp(sysResps);

            if(typeSresps!=""){
                type =typeSresps;
            }else{
                type = " sysRespPos";
                if(isPassiveVoiceSr(segmentTagged)){
                    type += "PassiveSR";
                    String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        type += "WithAmbVerbSR";
                    }
                }else{
                    String mainVerb = getVerbSegmentSR(segmentTagged);
                    String stemmedVerb = Utils.wordStemmer(mainVerb);
                    if (FindSmell.notPreciseVerb(stemmedVerb)) {
                        type +="WithAmbVerbSr";
                    }
                }
            }
        }else if(PosMethods.isSegSRespIncorrect(segmentTagged).contains("SysRespIncompl") ||
                PosMethods.isSegSRespIncorrect(segmentTagged).equals("SysRespIncorrect")){
            String typeIncomp = PosMethods.isSegSRespIncorrect(segmentTagged);
            if(typeIncomp.toLowerCase().contains("sysrespincomplnomodal")||
                    typeIncomp.toLowerCase().contains("sysrespincomplnomodalnoverb")||
                    typeIncomp.toLowerCase().contains("sysrespincomplnonounnomodal")){
                if(typePrevSeg.toLowerCase().contains("cond")){
                    if(typeIncomp.toLowerCase().contains("sysrespincomplnomodal")){
                        type = " condPosIncomplNoSubConj";
                    }else if(typeIncomp.toLowerCase().contains("sysrespincomplnomodalnoverb")){
                        type = " condPosIncomplNoSubConjNoVerb";
                    }else  if(typeIncomp.toLowerCase().contains("sysrespincomplnonounnomodal")){
                        type = " condPosIncomplNoSubConjNoNoun";
                    }
                }else if(typePrevSeg.toLowerCase().contains("sysresp")||
                        typePrevSeg.toLowerCase().contains("srs")){
                    type = " "+typeIncomp;
                }
            }else if(typeIncomp.toLowerCase().contains("sysrespincomplnonoun")){
                type = " "+typeIncomp;
            }else{
                type = " "+typeIncomp;
            }

            if(typeIncomp.toLowerCase().contains("sysrespincomplnomodal")
                    || typeIncomp.toLowerCase().contains("sysrespincomplnonoun")||
                    typeIncomp.toLowerCase().contains("sysrespincomplnonounnomodal")){

                if(isPassiveVoice(segmentTagged)){
                    if(typePrevSeg.toLowerCase().contains("sysresp")||typePrevSeg.toLowerCase().contains("srs")){
                        type += "PassiveSR";
                    }else if(type.contains("cond")){
                        type += "PassiveCond";
                    }

                    String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        type += "WithAmbVerb";
                    }

                }else{
                    String mainVerb = getVerbSegmentSR(segmentTagged);
                    String stemmedVerb = Utils.wordStemmer(mainVerb);
                    if (FindSmell.notPreciseVerb(stemmedVerb)) {
                        type +="WithAmbVerb";
                    }
                }
            }
        }else if(PosMethodsCondition.isSegmentCondS8(segmentTagged)){
            //"VB","VBD","VBP","VBZ"
            ArrayList<Condition> conds = PosMethodsCondition.detectMultCondReq(segmentTagged);
            String typeConds = PosMethodsCondition.getTypeMultCondBullet(conds);
            boolean hasCoord = PosMethods.checkCoordAmbCond(conds,typeConds);

            if(typeConds!=""){
                if(hasCoord){
                    type +=" "+typeConds+"CoordAmb";
                }else {
                    type +=" "+typeConds;
                }
            }else{
                type = " conditionPosS8";
                if(isPassiveVoice(segmentTagged)){
                    type += "PassiveCond";
                    String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    verbCondition = stemmedVerb;

                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        type += "WithAmbVerb";
                    }
                }else{


                    int position = PosMethodsCondition.getVerbInCond(segmentTagged);
                    if(position !=-1){

                        String mainVerb = segmentTagged.get(position).word();
                        if(mainVerb.toLowerCase().contains("is") || mainVerb.toLowerCase().contains("are") ||
                                mainVerb.toLowerCase().contains("has") ||mainVerb.toLowerCase().contains("have")){
                            if(position+1 <= segmentTagged.size()){
                                //if it has auxiliar verb
                                if(segmentTagged.get(position+1).tag().contains("vb")){
                                    mainVerb =  segmentTagged.get(position+1).word();
                                }
                            }
                        }

                        String stemmedVerb = Utils.wordStemmer(mainVerb);


                        verbCondition = stemmedVerb;

                        if (FindSmell.notPreciseVerb(stemmedVerb)) {
                            type +="WithAmbVerb";
                        }

                    }

                }
            }
        }else if(PosMethodsCondition.isSegCondIncorrect(segmentTagged).contains("condPosIncompl") ||
                PosMethodsCondition.isSegCondIncorrect(segmentTagged).equals("condIncorrect")){
            type += " "+PosMethodsCondition.isSegCondIncorrect(segmentTagged);
            String typeError = PosMethodsCondition.isSegCondIncorrect(segmentTagged);
            if(typeError.contains("condposincomplnosubconjnonoun")||
                    typeError.toLowerCase().contains("condposincomplnonoun")){
                if(isPassiveVoice(segmentTagged)){
                    type += "PassiveCond";
                    String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    verbCondition = stemmedVerb;
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        type += "WithAmbVerb";
                    }
                }else{

                    int position = PosMethodsCondition.getVerbInCond(segmentTagged);
                    if(position !=-1){

                        String mainVerb = segmentTagged.get(position).word();
                        if(mainVerb.toLowerCase().contains("is") || mainVerb.toLowerCase().contains("are") ||
                                mainVerb.toLowerCase().contains("has") ||mainVerb.toLowerCase().contains("have")){
                            if(position+1 <= segmentTagged.size()){
                                //if it has auxiliar verb
                                if(segmentTagged.get(position+1).tag().contains("vb")){
                                    mainVerb =  segmentTagged.get(position+1).word();
                                }
                            }
                        }

                        String stemmedVerb = Utils.wordStemmer(mainVerb);
                        verbCondition = stemmedVerb;

                        if (FindSmell.notPreciseVerb(stemmedVerb)) {
                            type +="WithAmbVerb";
                        }

                    }

                }
            }

    }else if(PosMethodsCondition.isSegmentCondS10(segmentTagged)){
            type = "conditionPosS10";
            if(isPassiveVoice(segmentTagged)){
                type += "PassiveCond";

                String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);
                String stemmedVerb = Utils.wordStemmer(mainVb);
                verbCondition = stemmedVerb;
                if(FindSmell.notPreciseVerb(stemmedVerb)){
                    type += "WithAmbVerb";
                }

            }else{
                int position = PosMethodsCondition.getVerbInCond(segmentTagged);
                if(position !=-1){

                    String mainVerb = segmentTagged.get(position).word();
                    if(mainVerb.toLowerCase().contains("is") || mainVerb.toLowerCase().contains("are") ||
                            mainVerb.toLowerCase().contains("has") ||mainVerb.toLowerCase().contains("have")){
                        if(position+1 <= segmentTagged.size()){
                            //if it has auxiliar verb
                            if(segmentTagged.get(position+1).tag().contains("vb")){
                                mainVerb =  segmentTagged.get(position+1).word();
                            }
                        }
                    }

                    String stemmedVerb = Utils.wordStemmer(mainVerb);
                    verbCondition = stemmedVerb;

                    if (FindSmell.notPreciseVerb(stemmedVerb)) {
                        type +="WithAmbVerb";
                    }

                }

            }
        }else if(PosMethodsCondition.isSegmentCondS8Sub(segmentTagged)){
            type=" conditionPosS8";
            if(isPassiveVoice(segmentTagged)){
                type += "PassiveCond";
                String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);
                String stemmedVerb = Utils.wordStemmer(mainVb);
                verbCondition = stemmedVerb;
                if(FindSmell.notPreciseVerb(stemmedVerb)){
                    type += "WithAmbVerb";
                }
            }else{


                int position = PosMethodsCondition.getVerbInCond(segmentTagged);
                if(position !=-1){


                    String mainVerb = segmentTagged.get(position).word();
                    if(mainVerb.toLowerCase().contains("is") || mainVerb.toLowerCase().contains("are") ||
                            mainVerb.toLowerCase().contains("has") ||mainVerb.toLowerCase().contains("have")){
                        if(position+1 <= segmentTagged.size()){
                            //if it has auxiliar verb
                            if(segmentTagged.get(position+1).tag().contains("vb")){
                                mainVerb =  segmentTagged.get(position+1).word();
                            }
                        }
                    }

                    String stemmedVerb = Utils.wordStemmer(mainVerb);
                    verbCondition = stemmedVerb;
                    if (FindSmell.notPreciseVerb(stemmedVerb)) {
                        type +="WithAmbVerb";
                    }
                }
            }
        }else if(PosMethodsCondition.isSegmentCondAsSoon(segmentTagged)){
            type = " conditionPosAs";
            Condition condAs = PosMethodsCondition.getSegmentCondAsSoon(segmentTagged);
            if(condAs.getpVoice()){
                type += "PassiveCond";
                condAs.getTextVerb();
                String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);
                String stemmedVerb = Utils.wordStemmer(mainVb);
                verbCondition = stemmedVerb;
                if(FindSmell.notPreciseVerb(stemmedVerb)){
                    type += "WithAmbVerb";
                }
            }else{


                int position = PosMethodsCondition.getVerbInCond(segmentTagged);
                if(position !=-1){

                    String mainVerb = segmentTagged.get(position).word();
                    if(mainVerb.toLowerCase().contains("is") || mainVerb.toLowerCase().contains("are") ||
                            mainVerb.toLowerCase().contains("has") ||mainVerb.toLowerCase().contains("have")){
                        if(position+1 <= segmentTagged.size()){
                            //if it has auxiliar verb
                            if(segmentTagged.get(position+1).tag().contains("vb")){
                                mainVerb =  segmentTagged.get(position+1).word();
                            }
                        }
                    }

                    String stemmedVerb = Utils.wordStemmer(mainVerb);
                    verbCondition = stemmedVerb;

                    if (FindSmell.notPreciseVerb(stemmedVerb)) {
                        type +="WithAmbVerb";
                    }
                }
            }

        }else {
            type = "NotMatch";
        }
        s.setTypeOfClause(type);
        if(verbCondition.length()>0){
            s.setVerbCondition(verbCondition);
        }
        if(!type.equals("NotMatch")){
            s.setIsMatched(true);
        }
        return s;
    }

    //get number of segments/clauses that were succesfully identified
    public static Integer getValidTypes(SegmentReq seg){
        String [] types = seg.getTypeOfClause().split(" ");
        int counterValidWords = 0;
        for(String t:types){
            if(t.toLowerCase().trim()!="notmatch"){
                if(t.length()>1){
                    counterValidWords++;
                }
            }
        }
        return counterValidWords;
    }


    //assign the not matched clause/segment to the nearest segment/clause that was succesfully identified
    public static List<SegmentReq> notMatchedNearestComp(List<SegmentReq> r, String preprReq, MaxentTagger tagger, String reqID){

        List<SegmentReq> newSeg = new ArrayList<>();
        for(int i =0; i< r.size(); i++){
            String currentType = r.get(i).getTypeOfClause();
            if(currentType.trim().length() > 0 && currentType.toLowerCase() != "notmatch"){
                if (i+1 < r.size()){
                    if (r.get(i+1).getTypeOfClause().toLowerCase().contains("notmatch")){
                        ArrayList<TaggedWord> clauseTag = getTagSentence(Utils.getStringFromSegment(r.get(i+1).getStartSegment(),r.get(i+1).getEndSegment(),preprReq),tagger);

                        if(currentType.toLowerCase().contains("sysresp")){
                            Sresp clause = typeSrBullet(clauseTag);
                            if(clause.getVerb()){
                                r.get(i+1).setTypeOfClause("sysrespCont");
                            }

                        }else if(currentType.toLowerCase().contains("cond")){
                            if(clauseTag.size() >= 2){
                                if(clauseTag.get(0).toString().toLowerCase().trim().contains("and") ||
                                        clauseTag.get(1).toString().toLowerCase().trim().contains("and")){
                                    //check is there are multiple conditions? if there are many ands
                                    if(hasSegActionsAndSymbol(clauseTag)){
                                        //for multiple conditions
                                        ArrayList<Condition> conds = PosMethodsCondition.getSingleActionsCond(clauseTag);
                                        String typeCondCont = PosMethodsCondition.getTypeMultCondContinuation(conds);
                                        r.get(i+1).setTypeOfClause(typeCondCont);
                                    }else{
                                        //for single condition
                                        Condition cond = PosMethodsCondition.getTypeCondBullet(clauseTag);
                                        if((cond.getVerb()||cond.getVerbSymbol()) &&
                                                (clauseTag.get(0).toString().toLowerCase().trim().contains(",")
                                                        || clauseTag.get(0).toString().toLowerCase().trim().contains("and") )){

                                            String typeCondCont = "condCont";
                                            if(cond.getVerbSymbol()){
                                                typeCondCont+="WithNoVerb";
                                            }
                                            if(cond.getpVoice()){
                                                typeCondCont+="WithPassive";
                                            }
                                            if(cond.getAmbVerb()){
                                                typeCondCont+="WithAmbVerb";
                                            }
                                            r.get(i+1).setTypeOfClause(typeCondCont);
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        newSeg.addAll(r);
        return newSeg;
    }

    // Given a segments/clauses that were not matched by any tregex pattern, this method analyzes these not matched segments/clauses
    // to identify condition(s),  scope(s), and system response(s)
    public static List<SegmentReq> labelNotMatchSegs(List<SegmentReq> notMatchSegments, String preprReq, MaxentTagger tagger, String reqID){

        int countSeg = 0;
        String typePrevSeg = "";

        if(notMatchSegments.size() > 0 ) {
            for(SegmentReq seg: notMatchSegments){
                String notMatchedPart = Utils.getStringFromSegment(seg.getStartSegment(), seg.getEndSegment(), preprReq);

                //First we check if there are more than one segment inside a Row. Using the method
                // PosMethods.getTypesInsideSeg()
                seg = PosMethods.getTypesInsideSeg(seg, tagger, preprReq, reqID, typePrevSeg);
                typePrevSeg = seg.getTypeOfClause();




                if(!seg.getTypeOfClause().toLowerCase().trim().contains("notmatch") && getValidTypes(seg)  >= 1)
                {
                    seg.setIsMatched(true);


                }else if(seg.getTypeOfClause().toLowerCase().trim().contains("notmatch")){
                    //Second: Then we check if there is only a segment in a row
                    //Here we only check fo the condition as soon as
                    if(countSeg==0) {
                        seg = getTypeOfSegment(seg, tagger, preprReq, reqID, typePrevSeg);
                        typePrevSeg = seg.getTypeOfClause();


                    }else {
                        seg = getTypeOfSegment(seg,tagger, preprReq, reqID, typePrevSeg);
                        typePrevSeg = seg.getTypeOfClause();


                    }
                }
                countSeg++;
            }
        }
        return notMatchSegments;
    }

    //check coordination ambiguity in conditions. Input any string
    public static Boolean checkCoordAmbCond(ArrayList<Condition> segments,String typeConds){
        boolean hasCoordAmb = false;
        boolean isAdditionalInfor = false;
        boolean complCond = false;
        if(segments.size() >= 2 ){
            ArrayList<TaggedWord> condsBef = segments.get(0).getConditionTagged();
            if(condsBef.size()>0) {
                if (condsBef.get(0).word().toLowerCase().contains("i.e.")) {
                    isAdditionalInfor = true;

                }
                if (condsBef.get(0).word().toLowerCase().contains("e.g.")) {
                    isAdditionalInfor = true;
                }
            }

        }

        if(typeConds.contains("mult") && !isAdditionalInfor && segments.size() >= 2 ){
            for(int j=0; j< segments.size(); j++){
                String typeSingleCond = PosMethodsCondition.getSingleCondBulletType(segments.get(j));

                if(PosMethodsCondition.getSingleCondBulletType(segments.get(j)).toLowerCase().contains("completecond")){
                    complCond=true;
                }
                if(PosMethods.hasSubConj(segments.get(j).getConditionTagged()) || typeSingleCond.toLowerCase().contains("completecond") ||
                        typeSingleCond.toLowerCase().contains("incomplcond") ){


                    String last = "";
                    String befLast = "";
                    String firstWord = "";
                    String secWord = "";
                    if(segments.get(j).getConditionTagged().size() > 0){
                        last = segments.get(j).getConditionTagged().get(segments.get(j).getConditionTagged().size()-1).word().toLowerCase();
                        firstWord = segments.get(j).getConditionTagged().get(0).word().toLowerCase();
                    }
                    if(segments.get(j).getConditionTagged().size() > 1){
                        befLast= segments.get(j).getConditionTagged().get(segments.get(j).getConditionTagged().size()-2).word().toLowerCase();
                        secWord =  segments.get(j).getConditionTagged().get(1).word().toLowerCase();
                    }

                    //Check in the last element
                    if(j==(segments.size()-1)){
                        //Check that there is a complete cond before
                        if(complCond){
                            if(firstWord.trim().equals("or")|| firstWord.trim().equals("and/or")|| firstWord.contains(",or")){

                                hasCoordAmb = true;
                            }else if(secWord.trim().equals("or")||secWord.trim().equals("and/or")||secWord.trim().equals(",or")){
                                hasCoordAmb = true;
                            }
                        }
                    }


                    //or if | , if  |,or if  | , And If
                    if(last.equals("or") || last.equals("and/or") || last.contains(",or")){
                        if(j+1 < segments.size()){

                            if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged()) ||
                                    PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond")){

                                hasCoordAmb = true;
                            }

                        }

                    }else if(last.contains(",") && befLast.toLowerCase().equals("or")){
                        if(j+1 < segments.size()){

                            if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged()) ||
                                    PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond") ){

                                hasCoordAmb = true;
                            }
                        }
                    }else if(firstWord.trim().equals("or") || firstWord.trim().equals("and/or") || firstWord.contains(",or")){
                        if(j+1 < segments.size()) {

                            if (PosMethods.hasSubConj(segments.get(j + 1).getConditionTagged()) ||
                                    PosMethodsCondition.getSingleCondBulletType(segments.get(j + 1)).toLowerCase().contains("cond")) {

                                hasCoordAmb = true;
                            }
                        }
                    }else if(secWord.equals("or")||secWord.equals("and/or")||secWord.equals(",or")){
                        if(j+1 < segments.size()) {

                            if (PosMethods.hasSubConj(segments.get(j + 1).getConditionTagged()) ||
                                    PosMethodsCondition.getSingleCondBulletType(segments.get(j + 1)).toLowerCase().contains("cond")) {

                                hasCoordAmb = true;
                            }
                        }
                    }
                    else if((last.trim().length()>2 && !FindSmell.isAnd(last)) && (befLast.toLowerCase().equals("or")|| befLast.toLowerCase().equals("and/or")) ){
                        if(j+1 < segments.size()){

                            if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged()) ||
                                    PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond")){

                                hasCoordAmb = true;
                            }
                        }
                    }else if( (last.trim().length()>2 &&!FindSmell.isAnd(last)) && (befLast.toLowerCase().equals(",or")|| befLast.toLowerCase().equals(",and/or") )){
                        if(j+1 < segments.size()){

                            if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged())||
                                    PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond")){

                                hasCoordAmb = true;
                            }
                        }
                    }

                }
            }
        }
        return hasCoordAmb;
    }

    //Check coordination ambiguity in conditon the input should be tagged words
    public static Boolean checkCoordAmbCondSegTag(ArrayList<ArrayList<TaggedWord>> segments){
        boolean hasCoordAmb = false;
        boolean isAdditionalInfor = false;
        boolean complCond = false;

        if(segments.size() > 2 ){

            ArrayList<TaggedWord> condsBef = segments.get(0);
            if(condsBef.get(0).word().toLowerCase().contains("i.e.")){
                isAdditionalInfor = true;

            }
            if(condsBef.get(0).word().toLowerCase().contains("e.g.")){
                isAdditionalInfor = true;
            }
        }
        if(!isAdditionalInfor && segments.size() > 2){
            for(int j=0; j<segments.size(); j++){
                int indexVerb = getPositionContains(segments.get(j), "/VB");

                //Checks that either has a subordinated conjunction or verb
                if(PosMethods.hasSubConj(segments.get(j)) || indexVerb!=-1 ){


                    String last = "";
                    String befLast = "";
                    String firstWord = "";
                    String secWord = "";
                    if(segments.get(j).size() > 0){
                        last = segments.get(j).get(segments.get(j).size()-1).word().toLowerCase();
                        firstWord = segments.get(j).get(0).word().toLowerCase();
                    }
                    if(segments.get(j).size() > 1){
                        befLast= segments.get(j).get(segments.get(j).size()-2).word().toLowerCase();
                        secWord =  segments.get(j).get(1).word().toLowerCase();
                    }
                    //or if | , if  |,or if  | , And If and/or"
                    if(last.equals("or") || last.equals("and/or")|| last.contains(",or")){
                        if(j+1 < segments.size()){
                            if(PosMethods.hasSubConj(segments.get(j+1)) ){
                                hasCoordAmb = true;
                            }
                        }

                    }else if(last.contains(",") && (befLast.toLowerCase().equals("or") || befLast.toLowerCase().equals("and/or") )){
                        if(j+1 < segments.size()){
                            if(PosMethods.hasSubConj(segments.get(j+1))){
                                hasCoordAmb = true;
                            }
                        }
                    }else if(firstWord.equals("or") || firstWord.equals("and/or")|| firstWord.contains(",or")){
                        if(j+1 < segments.size()) {
                            if (PosMethods.hasSubConj(segments.get(j + 1)) ) {
                                hasCoordAmb = true;
                            }
                        }
                    }else if(secWord.equals("or")||secWord.equals("and/or")||secWord.equals(",or")){
                        if(j+1 < segments.size()) {
                            if (PosMethods.hasSubConj(segments.get(j + 1)) ) {
                                hasCoordAmb = true;
                            }
                        }
                    }else if((last.trim().length()>2 && !FindSmell.isAnd(last)) && (befLast.toLowerCase().equals("or")||befLast.toLowerCase().equals("and/or") )){
                        if(j+1 < segments.size()){
                            if(PosMethods.hasSubConj(segments.get(j+1))){

                                hasCoordAmb = true;
                            }
                        }
                    }else if((last.trim().length()>2 && !FindSmell.isAnd(last)) && (befLast.toLowerCase().equals(",or")|| befLast.toLowerCase().equals(",and/or"))){
                        if(j+1 < segments.size()){
                            if(PosMethods.hasSubConj(segments.get(j+1))){

                                hasCoordAmb = true;
                            }
                        }
                    }

                }
            }

        }

        return hasCoordAmb;
    }

    // Method that analyzes a segment to identify other segments/clauses using the method ""getTypesInsideSeg"
    public static List<SegmentReq> getTypesInsideSegments(List<SegmentReq> req, MaxentTagger tagger, String cleanReq, String reqID){


        String typeSegment = "";
        int counterSegs = 0;
        for(SegmentReq s:req){
            if(counterSegs == 0){
                s = getTypesInsideSeg(s,tagger,cleanReq,reqID, "");
                typeSegment = s.getTypeOfClause();
            }else{
                s = getTypesInsideSeg(s,tagger,cleanReq,reqID, typeSegment);
                typeSegment = s.getTypeOfClause();

            }
            counterSegs ++;
        }
        return req;
    }



    // Method that analyzes a segment to identify other segments/clauses that can be within the initial segment
    public static SegmentReq  getTypesInsideSeg(SegmentReq s, MaxentTagger tagger, String cleanReq, String reqID, String prevType){
        String actualType = s.getTypeOfClause();
        String newType = "";
        String actualPVoiceCond = s.getPassiveVoiceCond();
        String actualPVoiceSr = s.getPassiveVoiceSR();
        String seg = Utils.getStringFromSegment(s.getStartSegment(),s.getEndSegment(),cleanReq);
        ArrayList<ArrayList<TaggedWord>> newSegments = splitSegments(PosMethods.separateBullet(seg),tagger);

        //All Types
        ArrayList<SegmentAndType> segsInSent = new ArrayList<>();


        String verbCondition = "";

        if(newSegments.size() > 1 && !actualType.contains("mult")){
            for(ArrayList<TaggedWord> newSeg : newSegments){
                if(newSeg.size()>1 && !(newSeg.get(0).toString().toLowerCase().equals("i.e/fw") ||newSeg.get(0).toString().toLowerCase().equals("i.e./fw")  )){

                    if(isSegmentScope(newSeg)){
                        newType += " ScopePos";
                        SegmentAndType scope =  new SegmentAndType(newSeg,"ScopePos");
                        segsInSent.add(scope);


                    }else if(PosMethods.isSegScopeIncorrect(newSeg).equals("ScopeIncorr") || PosMethods.isSegScopeIncorrect(newSeg).equals("ScopeIncompl")){
                        newType += " "+isSegScopeIncorrect(newSeg); //ScopePosIncomplete";
                        SegmentAndType scopeInc =  new SegmentAndType(newSeg,"ScopeIncorr");
                        segsInSent.add(scopeInc);


                    }
                    if(isSegmentSR(newSeg, false)){


                        ArrayList<Sresp> sysResps = PosMethods.detectMultSysResponses(newSeg);
                        String typeSresps = PosMethods.getTypeMultSresp(sysResps);

                        if(typeSresps!=""){
                            newType +=" "+typeSresps;
                            SegmentAndType sResp =  new SegmentAndType(newSeg,typeSresps);
                            segsInSent.add(sResp);
                        }else{
                            newType += " SysRespPos";

                            if(isPassiveVoiceSr(newSeg)){
                                actualPVoiceSr += " PassiveSR";
                                s.setPassiveVoiceSR(actualPVoiceSr);

                                String mainVb = PosMethods.getVerbPassiveVoiceSr(newSeg);
                                String stemmedVerb = Utils.wordStemmer(mainVb);
                                if(FindSmell.notPreciseVerb(stemmedVerb)){
                                    newType += "WithAmbVerb";
                                }
                            }else{
                                String mVerb = getVerbSegmentSR(newSeg);
                                String stemmedVerb = Utils.wordStemmer(mVerb);
                                if(FindSmell.notPreciseVerb(stemmedVerb)){
                                    newType += "WithAmbVerb";
                                }
                            }
                            SegmentAndType sResp =  new SegmentAndType(newSeg,"SysRespPos");
                            segsInSent.add(sResp);
                        }



                    }else if(PosMethods.isSegSRespIncorrect(newSeg).contains("SysRespIncompl") ||PosMethods.isSegSRespIncorrect(newSeg).equals("SysRespIncorrect")){

                        String typeIncomp = PosMethods.isSegSRespIncorrect(newSeg);
                        String smell = "";

                        if(typeIncomp.toLowerCase().contains("sysrespincomplnomodal")||
                                typeIncomp.toLowerCase().contains("sysrespincomplnomodalnoverb")||
                                typeIncomp.toLowerCase().contains("sysrespincomplnonounnomodal")){
                            if(prevType.toLowerCase().contains("cond")){
                                if(typeIncomp.toLowerCase().contains("sysrespincomplnomodal")){
                                    newType += " condPosIncomplNoSubConj";
                                }else if(typeIncomp.toLowerCase().contains("sysrespincomplnomodalnoverb")){
                                    newType += " condPosIncomplNoSubConjNoVerb";
                                }else  if(typeIncomp.toLowerCase().contains("sysrespincomplnonounnomodal")){
                                    newType += " condPosIncomplNoSubConjNoNoun";
                                }
                            }else if(prevType.toLowerCase().contains("sysresp")||
                                    prevType.toLowerCase().contains("srs")){
                                newType += " "+typeIncomp;
                            }
                        }else if(typeIncomp.toLowerCase().contains("sysrespincomplnonoun")){
                            newType += " "+typeIncomp;
                        }else{
                            newType += " "+typeIncomp;
                        }

                        if(typeIncomp.toLowerCase().contains("sysrespincomplnomodal")
                                || typeIncomp.toLowerCase().contains("sysrespincomplnonoun")||
                                typeIncomp.toLowerCase().contains("sysrespincomplnonounnomodal")){
                            if(isPassiveVoice(newSeg)){
                                if(prevType.toLowerCase().contains("cond")){
                                    actualPVoiceCond += " PassiveCond";
                                    s.setPassiveVoiceCond(actualPVoiceCond);
                                }else if(prevType.toLowerCase().contains("sysresp")||prevType.toLowerCase().contains("srs")){
                                    actualPVoiceSr += " PassiveSR";
                                    s.setPassiveVoiceSR(actualPVoiceSr);
                                }
                                String mainVb = PosMethods.getVerbPassiveVoice(newSeg);
                                String stemmedVerb = Utils.wordStemmer(mainVb);
                                if(FindSmell.notPreciseVerb(stemmedVerb)){
                                    smell += "WithAmbVerb";
                                }
                            }else{
                                String mVerb = getVerbSegmentSR(newSeg);
                                String stemmedVerb = Utils.wordStemmer(mVerb);
                                if(FindSmell.notPreciseVerb(stemmedVerb)){
                                    smell += "WithAmbVerb";
                                }
                            }

                        }
                        newType += smell;// " SysRespPosIncomplete";;
                        SegmentAndType sRespInc =  new SegmentAndType(newSeg,PosMethods.isSegSRespIncorrect(newSeg));
                        segsInSent.add(sRespInc);

                    }



                    if(PosMethodsCondition.isSegmentCondS8(newSeg)){
                        ArrayList<Condition> conds = PosMethodsCondition.detectMultCondReq(newSeg);
                        String typeConds  = PosMethodsCondition.getTypeMultCondBullet(conds);


                        boolean hasCoord = PosMethods.checkCoordAmbCond(conds,typeConds);
                        if(typeConds!=""){
                            if(hasCoord){
                                newType +=" "+typeConds+"CoordAmb";
                            }else {
                                newType +=" "+typeConds;
                            }
                            SegmentAndType sRespInc =  new SegmentAndType(newSeg,typeConds);
                            segsInSent.add(sRespInc);

                        }else{
                            Condition condPos  = PosMethodsCondition.getSegmentCondS8(newSeg);

                            newType += " conditionPosS8";


                            if(condPos.getpVoice()){
                                actualPVoiceCond += " PassiveCond";
                                s.setPassiveVoiceCond(actualPVoiceCond);
                                verbCondition = condPos.getTextVerb();


                                if(condPos.getAmbVerb()){
                                    newType += "WithAmbVerb";
                                }
                            }else{
                                verbCondition = condPos.getTextVerb();
                                if(condPos.getAmbVerb()) {
                                    newType += "WithAmbVerb";
                                }
                            }



                            if (verbCondition!= null){
                                String stemmedVerb = Utils.wordStemmer(verbCondition);
                            }


                            SegmentAndType sRespInc =  new SegmentAndType(newSeg,"conditionPosS8");
                            segsInSent.add(sRespInc);
                        }



                    }else if(PosMethodsCondition.isSegmentCondS10(newSeg)){

                        newType += " conditionPosS10";
                        String smell ="";
                        SegmentAndType sRespInc =  new SegmentAndType(newSeg,"conditionPosS10");
                        segsInSent.add(sRespInc);

                        if(isPassiveVoice(newSeg)){
                            actualPVoiceCond += " PassiveCond";
                            s.setPassiveVoiceCond(actualPVoiceCond);

                            String mainVb = PosMethods.getVerbPassiveVoice(newSeg);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            verbCondition = stemmedVerb;
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                smell += "WithAmbVerb";
                            }

                        }else{
                            String mVerb = getVerbSegmentSR(newSeg);
                            String stemmedVerb = Utils.wordStemmer(mVerb);
                            verbCondition = stemmedVerb;
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                smell += "WithAmbVerb";
                            }

                        }
                        newType += smell;



                    }else if(PosMethodsCondition.isSegmentCondS8Sub(newSeg)){

                        newType += " conditionPosS8";
                        String smell = "";
                        SegmentAndType sRespInc =  new SegmentAndType(newSeg,"conditionPosS8");
                        segsInSent.add(sRespInc);

                        if(isPassiveVoice(newSeg)){
                            actualPVoiceCond += " PassiveCond";
                            s.setPassiveVoiceCond(actualPVoiceCond);

                            String mainVb = PosMethods.getVerbPassiveVoice(newSeg);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            verbCondition = stemmedVerb;
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                smell += "WithAmbVerb";
                            }
                        }else{
                            String mVerb = getVerbSegmentSR(newSeg);
                            String stemmedVerb = Utils.wordStemmer(mVerb);
                            verbCondition = stemmedVerb;
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                smell += "WithAmbVerb";
                            }
                        }
                        newType += smell;

                        //Concepts


                    }else if(PosMethodsCondition.isSegmentCondS8VerbSymbol(newSeg)){
                        newType += " conditionWithSymbol";
                        SegmentAndType sRespInc =  new SegmentAndType(newSeg,"conditionWithSymbol");
                        segsInSent.add(sRespInc);

                    }else if(PosMethodsCondition.isSegmentCondModal(newSeg)){
                        newType += " conditionWithModal";
                        SegmentAndType sRespInc =  new SegmentAndType(newSeg,"conditionWithModal");
                        segsInSent.add(sRespInc);
                        String smell ="";

                        if(isPassiveVoice(newSeg)){
                            actualPVoiceCond += " PassiveCond";

                            s.setPassiveVoiceCond(actualPVoiceCond);

                            String mainVb = PosMethods.getVerbPassiveVoice(newSeg);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            verbCondition = stemmedVerb;
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                smell += "WithAmbVerb";
                            }
                        }


                    }else if(PosMethodsCondition.isSegCondIncorrect(newSeg).contains("condPosIncompl") ||
                            PosMethodsCondition.isSegCondIncorrect(newSeg).equals("condIncorrect")){



                        newType += " "+ PosMethodsCondition.isSegCondIncorrect(newSeg); // CondPosIncomplete";
                        String smell ="";
                        SegmentAndType sRespInc =  new SegmentAndType(newSeg,PosMethodsCondition.isSegCondIncorrect(newSeg));
                        segsInSent.add(sRespInc);
                        if(PosMethodsCondition.isSegCondIncorrect(newSeg).contains("condPosIncomplNoNoun")){
                            if(isPassiveVoice(newSeg)){
                                actualPVoiceCond += " PassiveCond";

                                s.setPassiveVoiceCond(actualPVoiceCond);

                                String mainVb = PosMethods.getVerbPassiveVoice(newSeg);
                                String stemmedVerb = Utils.wordStemmer(mainVb);
                                verbCondition = stemmedVerb;
                                if(FindSmell.notPreciseVerb(stemmedVerb)){
                                    smell += "WithAmbVerb";
                                }

                            }else{
                                String mVerb = getVerbSegmentSR(newSeg);
                                String stemmedVerb = Utils.wordStemmer(mVerb);
                                verbCondition = stemmedVerb;
                                if(FindSmell.notPreciseVerb(stemmedVerb)){
                                    smell += "WithAmbVerb";
                                }
                            }
                        }
                        newType += smell;
                    }
                }
            }
        }
        //newSegments
        ArrayList<ArrayList<TaggedWord>> subseqCond = new ArrayList<>();
        int temp = -1;
        for(int i=0; i< segsInSent.size();i++){
            String t = segsInSent.get(i).getTypeSeg();
            if(t.contains("cond") && !t.contains("mult")){
                if(temp ==-1){
                    temp = i;
                    subseqCond.add(segsInSent.get(i).getSentTagged());
                }else if((i-temp) ==1){
                    subseqCond.add(segsInSent.get(i).getSentTagged());
                    temp = i;
                }
            }
        }



        boolean hasCoord = false;
        if(subseqCond.size()>2){
            hasCoord = checkCoordAmbCondSegTag(subseqCond);
        }

        if(newType.trim().length()>1){
            if(isMultipleSR(newType) && !newType.toLowerCase().contains("multsrs")){
                String incompSR = getIncompSRs(newType);
                if(incompSR.trim().length()>1){
                    s.setTypeOfClause("multSrsPos"+" "+incompSR);
                }else{
                    s.setTypeOfClause("multSrsPos");
                }
            }else if( !newType.contains("multCond") && isMultipleCond(newType)){
                String incompCond = getIncompConds(newType);
                if(incompCond.trim().length()>1){

                    if(hasCoord){
                        s.setTypeOfClause("multCondPosCoordAmb"+" "+incompCond);
                    }else{
                        s.setTypeOfClause("multCondPos"+" "+incompCond);
                    }
                }else{
                    if(hasCoord){
                        s.setTypeOfClause("multCondPosCoordAmb");
                    }else{
                        s.setTypeOfClause("multCondPos");
                    }
                }
            }else{
                s.setTypeOfClause(newType);
                if(verbCondition!=null && verbCondition.length()>0){
                    s.setVerbCondition(verbCondition);
                }
            }
            s.setIsMatched(true);
        }
        return  s;
    }

    //returns incomplete conditions
    public static String getIncompConds(String type){
        String incompConds = "";
        String[] types = type.split(" ");
        if(types.length > 0){
            for(int i=0; i<types.length; i++){
                if(types[i].toLowerCase().contains("condposincompl")){
                    incompConds+="With"+types[i]+" ";
                }
            }
        }
        return incompConds.trim();
    }

    //returns incomplete system responses
    public static String getIncompSRs(String type){
        String incompSrs = "";
        String[] types = type.split(" ");
        if(types.length > 0){
            for(int i=0; i<types.length; i++){
                if(types[i].toLowerCase().contains("sysrespincompl")){
                    incompSrs+="With"+types[i]+" ";
                }
            }
        }
        return incompSrs.trim();
    }

    //detects multiple system responses
     public static Boolean isMultipleSR(String typeReq){
        boolean isMultSR = false;
        int countSR = 0;
        int countScope = 0;
        int countCond = 0;
        String[] types = typeReq.split(" ");
        for(String t:types){
            if(t.toLowerCase().contains("sysresp")){
                countSR++;
            }else if(t.toLowerCase().contains("scope")){
                countScope++;
            }else if(t.toLowerCase().contains("cond")){
                countCond++;
            }
        }
        if(countSR>1 && countScope==0 && countCond==0){
            isMultSR =true;
        }
        return isMultSR;
     }

     //detects multiple condition
    public static Boolean isMultipleCond(String typeReq){
        boolean isMultCond = false;
        int countSR = 0;
        int countScope = 0;
        int countCond = 0;
        String[] types = typeReq.split(" ");
        for(String t:types){
            if(t.toLowerCase().contains("sysresp")){
                countSR++;
            }else if(t.toLowerCase().contains("scope")){
                countScope++;
            }else if(t.toLowerCase().contains("cond")){
                countCond++;
            }
        }
        if(countCond>1 && countSR==0 && countScope==0){
            isMultCond = true;
        }

        return isMultCond;
    }




    //Method that given a requirement tries to identify the segments/clauses present on the string
    public static List<MatchPattern> getTypesSegmentAftAnalysis(String req, MaxentTagger tagger)
    {

        ArrayList<ArrayList<TaggedWord>> listsegments = splitSegments(PosMethods.separateBullet(req),tagger);
        List<MatchPattern> patternsReq = new ArrayList<>();

        int countSeg = 0;
        String prevType = "";

        for(ArrayList<TaggedWord> segment:listsegments){
            //Not considering i.e.
            if(segment.size()>1 && !(segment.get(0).toString().toLowerCase().equals("i.e/fw") || segment.get(0).toString().toLowerCase().equals("i.e./fw"))){

                boolean hasMultConds = false;
                boolean hasMultSrs = false;
                if(countSeg == 0){
                    prevType = "";
                }

                //detecting MultSRs
                ArrayList<Condition> conds = PosMethodsCondition.detectMultCondReq(segment);
                String typeConds  = PosMethodsCondition.getTypeMultCondBullet(conds);

                boolean hasCoord = PosMethods.checkCoordAmbCond(conds,typeConds);

                ArrayList<Sresp> srs = detectMultSysResponses(segment);
                String typeSresps = PosMethods.getTypeMultSresp(srs);
                ArrayList<TupleConcept> listConcepts = new ArrayList<>();

                if(typeConds.contains("mult")){

                    hasMultConds = true;
                }
                if(typeSresps.contains("mult")){

                    hasMultSrs = true;
                }

                if(isSegmentScope(segment)){

                    patternsReq.add(new MatchPattern(getTextFromSentence(segment), "scopePos"));
                    prevType = "scopePos";
                }else if(PosMethods.isSegScopeIncorrect(segment).equals("ScopeIncorr") ||PosMethods.isSegScopeIncorrect(segment).equals("ScopeIncompl" )){

                    patternsReq.add(new MatchPattern(getTextFromSentence(segment), PosMethods.isSegScopeIncorrect(segment)));
//                newType += " "+PosMethods.isSegScopeIncorrect(segment);// ScopePosIncomplete";
                    prevType = PosMethods.isSegScopeIncorrect(segment);

                }else if(hasMultSrs){

                    patternsReq.add(new MatchPattern(getTextFromSentence(segment),typeSresps));
                    prevType = typeSresps;
                }else if(isSegmentSR(segment, false)){

                    MatchPattern mp = new MatchPattern(getTextFromSentence(segment), "sysRespPos");
                    //Check if Passive Voice
                    List<TupleConcept> concepSR = conceptsInSR(segment);
                    if(isPassiveVoiceSr(segment)){
                        mp.setPassiveVoiceSR("PassiveSR");
                        String mainVb = PosMethods.getVerbPassiveVoiceSr(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        TupleConcept verbSr = new TupleConcept("verb", mainVb);
                        concepSR.add(verbSr);


                        if(FindSmell.notPreciseVerb(stemmedVerb)){
                            mp.setTypeOfPattern("sysRespPosWithAmbVerb");
                        }
                    }else{
                        String mainVb = getVerbSegmentSR(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        TupleConcept verbSr = new TupleConcept("verb", mainVb);
                        concepSR.add(verbSr);
                        if(FindSmell.notPreciseVerb(stemmedVerb)){
                            mp.setTypeOfPattern("sysRespPosWithAmbVerb");
                        }

                    }

                    //Adding concepts
                    mp.setConceptsInText(concepSR);
                    patternsReq.add(mp);
                    prevType = "sysRespPos";

                }else if(PosMethods.isSegSRespIncorrect(segment).contains("SysRespIncompl") ||PosMethods.isSegSRespIncorrect(segment).equals("SysRespIncorrect")){

//                newType += " "+PosMethods.isSegSRespIncorrect(segment);//SysRespPosIncomplete";
                    String type = PosMethods.isSegSRespIncorrect(segment);
                    String typeSeg = "";
                    if(type.toLowerCase().contains("sysrespincomplnomodal")||
                            type.toLowerCase().contains("sysrespIncomplnomodalnoverb")||
                            type.toLowerCase().contains("sysrespincomplnonounnomodal")){
                        if(prevType.contains("cond")){
                            if(type.toLowerCase().contains("sysrespincomplnomodal")){
                                typeSeg = "condPosIncomplNoSubConj" ;

                            }else if(type.toLowerCase().contains("sysrespIncomplnomodalnoverb")){
                                typeSeg = "condPosIncomplNoSubConjNoVerb";

                            }else if(type.toLowerCase().contains("sysrespincomplnonounnomodal")){
                                typeSeg = "condPosIncomplNoSubConjNoNoun";
                            }

                        }else if(prevType.contains("sysresp") || prevType.contains("srs")){
                            typeSeg = PosMethods.isSegSRespIncorrect(segment);

                        }

                    }else if(type.toLowerCase().contains("sysrespincomplnonoun")) {
                        typeSeg += " " + type;
                    }else{
                        typeSeg = " "+type;
                    }

                    MatchPattern mp = new MatchPattern(getTextFromSentence(segment), typeSeg);


                    if(type.toLowerCase().contains("sysrespincomplnomodal")
                            || type.toLowerCase().contains("sysrespincomplnonoun")||
                            type.toLowerCase().contains("sysrespincomplnonounnomodal")){
                        if(isPassiveVoice(segment)){
                            if(prevType.contains("cond")){
                                mp.setPassiveVoice("PassiveCond");
                            }else if(prevType.toLowerCase().contains("sysresp")||prevType.toLowerCase().contains("srs")){
                                mp.setPassiveVoiceSR("PassiveSR");
                            }
                            String mainVb = PosMethods.getVerbPassiveVoice(segment);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                mp.setTypeOfPattern(type+"WithAmbVerb");
                            }

                        }else {
                            String mainVb = getVerbSegmentSR(segment);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                mp.setTypeOfPattern(type+"WithAmbVerb");
                            }

                        }
                    }

                    patternsReq.add(mp);
                    prevType = typeSeg;
                } else if(hasMultConds) {

                    if(hasCoord){
                        patternsReq.add(new MatchPattern(getTextFromSentence(segment), typeConds+"CoordAmb"));
                    }else{
                        patternsReq.add(new MatchPattern(getTextFromSentence(segment), typeConds));
                    }

                    prevType = typeConds;
                }else if(PosMethodsCondition.isSegmentCondS8(segment)){

                    MatchPattern mp = new MatchPattern(getTextFromSentence(segment), "conditionPosS8");

                    if(isPassiveVoice(segment)) {
                        mp.setPassiveVoice("PassiveCond");

                        String mainVb = PosMethods.getVerbPassiveVoice(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        mp.setVerCondition(stemmedVerb);
                        if (FindSmell.notPreciseVerb(stemmedVerb)) {
                            mp.setTypeOfPattern("conditionPosS8WithAmbVerb");

                        }
                    }else{
                        String mainVb = getVerbSegmentSR(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        mp.setVerCondition(stemmedVerb);
                        if(FindSmell.notPreciseVerb(stemmedVerb)){
                            mp.setTypeOfPattern("conditionPosS8WithAmbVerb");
                        }
                    }
                    List<TupleConcept> concepCond = conceptsInCondS8(segment);
                    //Adding concepts
                    mp.setConceptsInText(concepCond);
                    patternsReq.add(mp);
                    prevType = "conditionPosS8";
                }else if(PosMethodsCondition.isSegmentCondS10(segment)){

                    MatchPattern mp = new MatchPattern(getTextFromSentence(segment), "conditionPosS10");
                    //Check if Passive Voice

                    if(isPassiveVoice(segment)){
                        mp.setPassiveVoice("PassiveCond");
                        String mainVb = PosMethods.getVerbPassiveVoice(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        mp.setVerCondition(stemmedVerb);
                        if (FindSmell.notPreciseVerb(stemmedVerb)) {
                            mp.setTypeOfPattern("conditionPosS8WithAmbVerb");

                        }
                    }else{
                        String mainVb = getVerbSegmentSR(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        mp.setVerCondition(stemmedVerb);
                        if(FindSmell.notPreciseVerb(stemmedVerb)){
                            mp.setTypeOfPattern("conditionPosS8WithAmbVerb");
                        }
                    }
                    List<TupleConcept> concepCond = conceptsInS10(segment);
                    mp.setConceptsInText(concepCond);
                    patternsReq.add(mp);
                    prevType = "conditionPosS10";
                }else if(PosMethodsCondition.isSegmentCondS8Sub(segment)){

                    MatchPattern mp = new MatchPattern(getTextFromSentence(segment), "conditionPosS8");
                    //Check if Passive Voice
                    if(isPassiveVoice(segment)){
                        mp.setPassiveVoice("PassiveCond");
                        String mainVb = PosMethods.getVerbPassiveVoice(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        mp.setVerCondition(stemmedVerb);
                        if (FindSmell.notPreciseVerb(stemmedVerb)) {
                            mp.setTypeOfPattern("conditionPosS8WithAmbVerb");

                        }
                    }else{
                        String mainVb = getVerbSegmentSR(segment);
                        String stemmedVerb = Utils.wordStemmer(mainVb);
                        mp.setVerCondition(stemmedVerb);
                        if(FindSmell.notPreciseVerb(stemmedVerb)){
                            mp.setTypeOfPattern("conditionPosS8WithAmbVerb");
                        }
                    }


                    //Concepts
                    List<TupleConcept> concS8Sub = conceptsS8Sub(segment);
                    mp.setConceptsInText(concS8Sub);
                    patternsReq.add(mp);
                    prevType = "conditionPosS8";
                }else  if(PosMethodsCondition.isSegCondIncorrect(segment).contains("condPosIncompl") || PosMethodsCondition.isSegCondIncorrect(segment).equals("condIncorrect")){


                    String type =  PosMethodsCondition.isSegCondIncorrect(segment);
                    MatchPattern mp = new MatchPattern(getTextFromSentence(segment),type);
                    if(PosMethodsCondition.isSegCondIncorrect(segment).contains("condPosIncomplNoNoun")){
                        if(isPassiveVoice(segment)){
                            mp.setPassiveVoice("PassiveCond");
                            String mainVb = PosMethods.getVerbPassiveVoice(segment);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            mp.setVerCondition(stemmedVerb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                mp.setTypeOfPattern(type+"WithAmbVerb");
                            }
                        }else{
                            String mVerb = getVerbSegmentSR(segment);
                            String stemmedVerb = Utils.wordStemmer(mVerb);
                            mp.setVerCondition(stemmedVerb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                mp.setTypeOfPattern(type+"WithAmbVerb");
                            }
                        }
                    }
                    patternsReq.add(mp);
                    prevType = type;

                }else{

                    patternsReq.add(new MatchPattern(getTextFromSentence(segment), "notMatchPos"));
                    prevType = "notMatchPos";
                }
                countSeg++;
            }else{
                patternsReq.add(new MatchPattern(getTextFromSentence(segment), "notMatchPos"));
                prevType = "notMatchPos";
                countSeg++;

            }

        }

        for (MatchPattern m: patternsReq){
            if(m.getTextMatch().contains("*")){
                m.setTextMatch(joinBullet(m.getTextMatch()));
            }
        }

        return patternsReq;
    }


    //detect separator of segment/clause
    public static Boolean isSepOfClause(TaggedWord word)
    {
        boolean hasSubConj = false;

        String patternSub = "\\b(When|when|where|Where|While|while|if|If|IF|Until|until|then|Then|For|for|<lb>|;|,|Else|else|Otherwise|otherwise|Once|once)\\b";
        //|i.e for now it has been removed
        if(PosMethods.MatchRegExpression(word.toString(), patternSub))
        {
            hasSubConj = true;
        }
        return hasSubConj;
    }

    //detect comma or semicolon
    public static Boolean isCommaSepClause(TaggedWord word){
        boolean hasSubConj = false;
        String patternSub = "^(,|;)";
        if(PosMethods.MatchRegExpression(word.toString(), patternSub))
        {
            hasSubConj = true;
        }
        return hasSubConj;

    }

    //count words that have more than 1 character
    public static Integer countValidWords(ArrayList<TaggedWord> sentence){
        int validWords = 0;
        for(TaggedWord w: sentence){
            if(w.word().trim().length() > 1){
                validWords++;
            }
        }
        return validWords;
    }


    //Returns the number of bullet points. In the preprocess we convert bullet point into "\*"
    public static int getNumberBullets(String sentence){
        int countBullets = 0;
        for(String w: PosMethods.separateBullet(sentence).split(" ")){
                if (w.equals("\\*")){
                    countBullets++;
                }
        }
        return countBullets;
    }

    //Method that split a segment/clause into sub segments/clauses. For example a condition within a system response
    public static ArrayList<ArrayList<TaggedWord>> splitSegments(String sentence, MaxentTagger tagger){
        boolean hasSubConj = false;
        ArrayList<TaggedWord> sentenceTags = getTagSentence(sentence,tagger);
        ArrayList<ArrayList<TaggedWord>> segmentsReq = new ArrayList<>();
        ArrayList<TaggedWord> segment = new ArrayList<>();

        //The sentence should not contain bullets
        if( sentence.toLowerCase().contains("following attributes") && getNumberBullets(sentence) > 1 ){
            segmentsReq.add(sentenceTags);

        }else{
            for (int i=0; i< sentenceTags.size(); i++){

                segment.add(sentenceTags.get(i));
                if(i+1 < sentenceTags.size()){
                    if(isSepOfClause(sentenceTags.get(i+1)) || isCommaSepClause(sentenceTags.get(i+1))){

                        if(sentenceTags.get(i+1).word().toLowerCase().trim().contains("for")){
                            if(sentenceTags.get(i).word().trim().contains(",")||sentenceTags.get(i).word().trim().contains(";")){
                                ArrayList<TaggedWord> temp = new ArrayList<>(segment);
                                if(countValidWords(temp) > 2){
                                    segmentsReq.add(temp);
                                    segment.clear();
                                }

                            }
                        }else if(sentenceTags.get(i+1).word().toLowerCase().trim().contains(",") && hasModalInPhrase(i+1, sentenceTags)){

                            if(hasModalInPhrase(i+1, sentenceTags)){
                                ArrayList<TaggedWord> temp = new ArrayList<>(segment);
                                if(countValidWords(temp) > 2){
                                    segmentsReq.add(temp);
                                    segment.clear();
                                }

                            }
                        }else if(sentenceTags.get(i+1).word().toLowerCase().trim().contains(",") && hasConditionNoVerbInPhrase(i+1,sentenceTags)){
                            if(hasConditionNoVerbInPhrase(i+1, sentenceTags)){
                                ArrayList<TaggedWord> temp = new ArrayList<>(segment);

                                if(countValidWords(temp) > 2){
                                    segmentsReq.add(temp);
                                    segment.clear();
                                }
                            }
                        }else if(sentenceTags.get(i+1).word().toLowerCase().trim().contains(",") && hasConditionOnce(i+1, sentenceTags)){
                            if(hasConditionOnce(i+1, sentenceTags)){
                                ArrayList<TaggedWord> temp = new ArrayList<>(segment);

                                if(countValidWords(temp) > 2){
                                    segmentsReq.add(temp);
                                    segment.clear();
                                }
                            }

                        }else if(sentenceTags.get(i+1).word().toLowerCase().trim().contains("until") && hasConditionUntil(i+1, sentenceTags)){
                            if(hasConditionUntil(i+1, sentenceTags)){
                                ArrayList<TaggedWord> temp = new ArrayList<>(segment);

                                if(countValidWords(temp) > 2){
                                    segmentsReq.add(temp);
                                    segment.clear();
                                }
                            }

                        }else if(!sentenceTags.get(i+1).word().toLowerCase().trim().contains(",") &&
                                !sentenceTags.get(i+1).word().toLowerCase().trim().contains("until") &&
                                !sentenceTags.get(i+1).word().toLowerCase().trim().contains("once") ){

                            ArrayList<TaggedWord> temp = new ArrayList<>(segment);
                            if(countValidWords(temp) > 2){
                                segmentsReq.add(temp);
                                segment.clear();
                            }
                        }
                    }
                }
                if(i == sentenceTags.size()-1){

                    segmentsReq.add(segment);
                }
            }

        }
        return segmentsReq;
    }

    //Checks if there is a modal after a certain position in a sentence
    public static boolean hasModalInPhrase(int p, List<TaggedWord> phrase){
        boolean hasModal = false;
        if( p+1 < phrase.size() ){
            for( int i=p+1; i < phrase.size(); i++ ){
                if(phrase.get(i).word().toLowerCase().trim().contains("must") ||
                        phrase.get(i).word().toLowerCase().trim().contains("shall") ||
                        phrase.get(i).word().toLowerCase().trim().contains("should")){

                    hasModal = true;
                    break;
                }else if (isSepOfClause(phrase.get(i)) || isCommaSepClause(phrase.get(i))){
                    if(!(phrase.get(i).toString().toLowerCase().equals("i.e/fw")||phrase.get(i).toString().toLowerCase().equals("i.e./fw"))){
                        hasModal = false;
                        break;
                    }
                }
            }
        }
        return hasModal;
    }


    // Checks for condition that starts with "on action(noun)" or "upon action(noun)", e.g., "upon receipt"
    public static boolean hasConditionNoVerbInPhrase(int p, List<TaggedWord> phrase){
        boolean hasModal = false;
        if( p+2 < phrase.size() ){
            for( int i=p+1; i < phrase.size(); i++ ){
                if(i+1 < phrase.size()){
                    if((phrase.get(i).word().toLowerCase().trim().equals("on") && phrase.get(i+1).tag().toLowerCase().trim().contains("nn"))||
                            (phrase.get(i).word().toLowerCase().trim().equals("upon") && phrase.get(i+1).word().toLowerCase().trim().contains("nn")) ){

                        hasModal = true;
                        break;
                    }else if (isSepOfClause(phrase.get(i)) || isCommaSepClause(phrase.get(i))){
                        if(!(phrase.get(i).toString().toLowerCase().equals("i.e/fw")||phrase.get(i).toString().toLowerCase().equals("i.e./fw"))){
                            hasModal = false;
                            break;
                        }
                    }
                }
            }
        }
        return hasModal;
    }

    //Detect keywork "until" used in a condition
    public static boolean hasConditionUntil(int p, List<TaggedWord> phrase){
        boolean hasUntil = false;
        if(p+1 < phrase.size()){
            if(phrase.get(p+1).word().toLowerCase().trim().equals("until")){
                for (int i= p+2; i < phrase.size(); i++){
                    //"VB","VBD","VBP","VBZ"
                    if(phrase.get(i).tag().toLowerCase().trim().equals("vb")||
                            phrase.get(i).tag().toLowerCase().trim().equals("vbd")||
                            phrase.get(i).tag().toLowerCase().trim().equals("vbp")||
                            phrase.get(i).tag().toLowerCase().trim().equals("vbz")){
                        hasUntil = true;
                    }
                }
            }
        }
        return hasUntil;
    }

    //Method that returns true if a condition start with the keyword "once"
    public static boolean hasConditionOnce(int p, List<TaggedWord> phrase){
        boolean hasUntil = false;
        if(p+1 < phrase.size()){
            if(phrase.get(p+1).word().toLowerCase().trim().equals("once")){
                for (int i= p+2; i < phrase.size(); i++){
                    //"VB","VBD","VBP","VBZ"
                    if(phrase.get(i).tag().toLowerCase().trim().equals("vb")||
                            phrase.get(i).tag().toLowerCase().trim().equals("vbd")||
                            phrase.get(i).tag().toLowerCase().trim().equals("vbp")||
                            phrase.get(i).tag().toLowerCase().trim().equals("vbz")){
                        if(!phrase.get(i).word().toLowerCase().equals("based")){
                            hasUntil = true;
                        }

                    }
                }
            }
        }
        return hasUntil;
    }



    // Method that retrieves multiple actions in a system response
    // Scenario where the multiple actions of the SR are separated by a break line
    public static ArrayList<Sresp> getSingleTagSR(ArrayList<TaggedWord> taggedSR, String sep1, String sep2){
        ArrayList<Sresp> actionsSR =  new ArrayList<>();
        ArrayList<TaggedWord> action = new ArrayList<>();
        boolean separator = false;

        for (int i=0; i< taggedSR.size(); i++){

            action.add(taggedSR.get(i));
            if(taggedSR.get(i).toString().contains(sep1)){
                if(i+1 < taggedSR.size()){
                    if(taggedSR.get(i+1).toString().contains(sep2)){
                        separator = true;
                    }
                }
            }
            if(separator){
                ArrayList<TaggedWord> temporal = new ArrayList<>(action);
                Sresp tempCond  = typeSrBullet(temporal);
                if(tempCond.getVerb()){
                    actionsSR.add(typeSrBullet(temporal));
                    action.clear();
                }
//                if(typeSrBullet(temporal)){
//                    actionsSR.add(typeSrBullet(temporal));
//                    action.clear();
//                }
                separator = false;

            }
            if(i == taggedSR.size()-1){
                Sresp tempCond = typeSrBullet(action);
                if(tempCond.getVerb()){
                    actionsSR.add(tempCond);
                }

            }
        }
        return actionsSR;
    }

    //Get the text from multiple condition with bullets
    public static ArrayList<ArrayList<TaggedWord>> getTextFromBullets(ArrayList<TaggedWord> bulletsCondition){
        ArrayList<ArrayList<TaggedWord>> conditions =  new ArrayList<>();
        int start = 0;
        int end = -1;
        for(int i= 0; i< bulletsCondition.size(); i++){

            if(bulletsCondition.get(i).word().contains("\\*")){
                end = i;
            }

            if(end!=-1){

                List<TaggedWord> singleCondition = bulletsCondition.subList(start, end);
                conditions.add(new ArrayList<>(singleCondition));
                start = end;
                end = -1;
            }
            if((i == bulletsCondition.size() - 1) && (end==-1)){

                List<TaggedWord> singleCondition = bulletsCondition.subList(start, bulletsCondition.size());

                conditions.add(new ArrayList<>(singleCondition));
                break;
            }
        }

        return conditions;
    }

    //Method that detects requirements with two or more system responses
    public static ArrayList<Sresp> detectMultSysResponses(ArrayList<TaggedWord>  tagSysResp){
        ArrayList<Sresp> sysResponses = new ArrayList<>();


        if(isSegmentSR(tagSysResp,false)){
            String textSR = getTaggedTextFromSentence(tagSysResp);
            //Scenario where the SR contains bullets
            if(textSR.contains("\\*")){

                ArrayList<ArrayList<TaggedWord>> segmentsSR = getTextFromBullets(tagSysResp);
                //typeSrBullet
                if(segmentsSR.size() > 1){
                    for(int i = 0; i < segmentsSR.size(); i++){
                        Sresp temp = typeSrBullet(segmentsSR.get(i));
                        sysResponses.add(typeSrBullet(segmentsSR.get(i)));
                    }
                    if (getTypeMultSresp(sysResponses).trim().length()==0){
                        if(checkCoorConjNotBullet(tagSysResp)){

                            if (hasSegActions(tagSysResp))
                            {

                                sysResponses  = getSingleActionsSR(tagSysResp);
                            }
                        }
                    }
                }//getTypeMultSresp


            }else if(textSR.contains("./. ,/,")){
                sysResponses = getSingleTagSR(tagSysResp,"./.",",/,");
                sysResponses = getSingleTagSR(tagSysResp,"./.",",/,");

            }else if(!(textSR.toLowerCase()).contains("./. ,/,") || !(textSR.toLowerCase()).contains("\\*")){
                if (hasSegActions(tagSysResp)){
                    sysResponses  = getSingleActionsSR(tagSysResp);

                }
            }
        }
        return sysResponses;
    }


    public static String preProcBullets(String s){
        String a = s.replace("\\*","\\* ");
        return a;
    }

    //Counts number of verbs in a condition
    public static int countVerbsInCond(ArrayList<TaggedWord> cond){
        int countVerbs = 0;
        for(TaggedWord w: cond){
            if(w.toString().contains("/VB")){
                countVerbs++;
            }
        }
        return countVerbs;
    }







    //Checks if a System response does not hav a modal verb
    public static List<SegmentReq> noModVerbSR(List<SegmentReq> partsReq, MaxentTagger tagger, String reqOrig){
        //First we check that there is a SR
        boolean hasSR = false;
        boolean hasCond = false;
        int posSR = -1;
        int posCond = -1;

        //getting a position of the last system response
        //getting a position of the last condition
        for(int i=0; i<partsReq.size(); i++){
            //the first system response and its position
            if(partsReq.get(i).getTypeOfClause().toLowerCase().contains("sysresp") ||
                    partsReq.get(i).getTypeOfClause().toLowerCase().contains("srs")){
                hasSR =true; //boolean if there is a system response
                posSR=i; //position of the first system response
            }
            if(partsReq.get(i).getTypeOfClause().toLowerCase().contains("cond")){
                hasCond = true;
                posCond = i;
            }

        }

        boolean noModVerbSR = false;
        for(int j=0; j<partsReq.size(); j++){

            if(partsReq.get(j).getTypeOfClause().toLowerCase().contains("notmatch")){
                String seg = Utils.getStringFromSegment(partsReq.get(j).getStartSegment(), partsReq.get(j).getEndSegment(),reqOrig);
                ArrayList<TaggedWord> segTag = PosMethods.getTagSentence(seg,tagger);
                int indexSubConj = firstPosSubConj(segTag);

                int indexPrepCond = firstPosPrepCondIncomp(segTag);
                int indexNoun = getPositionContains(segTag, "/N");
                int indexVerb = getPositionContains(segTag,"/VB");
                int indexModVerb = firstModalVerb(segTag);
                //Scenario missing modal verb
                if(indexNoun!=-1 && indexVerb != -1 && indexModVerb == -1 && indexSubConj == -1 && indexPrepCond == -1){
                    if(indexNoun < indexVerb)
                    {
                        int posNoModVbSr = j;
                        //the cluse should be after the SR and Cond
                        if(hasSR && hasCond && (posCond > posSR) && (posNoModVbSr > posSR) && (posNoModVbSr > posCond) ){
                            noModVerbSR = true;
                            partsReq.get(j).getErrorsSegment().setNoModalSR(true); //  reqSegError.setNoModalSR(true); .
                        }else if(!hasSR){
                            noModVerbSR = true;
                            partsReq.get(j).getErrorsSegment().setNoModalSR(true);
                        }
                    }
                }
            }
        }
        return partsReq;
    }


    //Check if a condition with no bullet points contains another condition separated by "and" or "or"
    public static Boolean checkCoorConjNotBullet(ArrayList<TaggedWord> r){
        boolean hasCoorConjNotBullet = false;
        ArrayList<ArrayList<TaggedWord>> segmentsSR = getTextFromBullets(r);
        //typeSrBullet
        if(segmentsSR.size() > 1){
            for (ArrayList<TaggedWord> clause: segmentsSR){

                String clauseText = getTaggedTextFromSentence(clause);

                //Check that the clause is not a bullet
                if(!clauseText.contains("\\*")){
                    outerloop:
                    for (TaggedWord w: clause){

                        if(w.toString().toLowerCase().contains("and/cc") ||
                                w.toString().toLowerCase().contains("or/cc"))
                        {

                            hasCoorConjNotBullet = true;
                            break outerloop;
                        }
                    }
                }
            }
        }
        return hasCoorConjNotBullet;
    }

    /*
    * Method that analyzes each condition within a requirement. A requirement has two or more condition using bullet points.
    * This method identifies smells that can be present in the condition, e.g., incomplete condition
    * */
    public static Sresp typeSrBullet(ArrayList<TaggedWord> segments){
        boolean isSRBullet = false;
        int indexSubConj = firstPosSubConj(segments);
        int indexModVerb = firstModalVerb(segments);
        int indexNoun = getPositionContains(segments, "/N");
        int prepIt = getPositionContains(segments,"it/PRP");

        if( (indexNoun == -1 && prepIt != -1 )  || ((prepIt != -1) && (prepIt < indexModVerb) )){
            indexNoun = getPositionContains(segments,"it/PRP");
        }


        int indexVerb = -1;
        int indexCand   = getPositionContains(segments, "and/CC");
        int indexCor    = getPositionContains(segments, "or/CC");

        String verb = "";
        if(indexModVerb!=-1){
            indexVerb = getPosVerbFromSpecLoc(segments, "VB", indexModVerb);
            verb = PosMethods.getVerbSpecLoc(segments,"VB", indexModVerb);

            boolean hasVerb = false;
            if (indexVerb == -1){
                indexVerb = getPosVerbFromSpecLoc(segments, "VBD", indexModVerb);
                verb = PosMethods.getVerbSpecLoc(segments,"VBD", indexModVerb);
                if(verb.trim().length() > 0){
                    hasVerb = true;
                }
                if(!hasVerb){
                    indexVerb = getPosVerbFromSpecLoc(segments, "VBP", indexModVerb);
                    verb = PosMethods.getVerbSpecLoc(segments,"VBP", indexModVerb);
                    if(verb.trim().length() > 0){
                        hasVerb = true;
                    }
                }
            }

        }else if (indexCand == 0 && indexCor != 0){

            indexVerb = getPosVerbFromSpecLoc(segments, "VB", indexCand);
            verb = PosMethods.getVerbSpecLoc(segments,"VB", indexCand);

            boolean hasVerb = false;
            if (indexVerb == -1){
                indexVerb = getPosVerbFromSpecLoc(segments, "VBD", indexCand);

                verb = PosMethods.getVerbSpecLoc(segments,"VBD", indexCand);

                if(verb.trim().length() > 0){
                    hasVerb = true;
                }
                if(!hasVerb){
                    indexVerb = getPosVerbFromSpecLoc(segments, "VBP", indexCand);

                    verb = PosMethods.getVerbSpecLoc(segments,"VBP", indexCand);

                    if(verb.trim().length() > 0){
                        hasVerb = true;
                    }
                }
            }


        }else if (indexCor == 0 && indexCand != 0){
            indexVerb = getPosVerbFromSpecLoc(segments, "VB", indexCor);
            verb = PosMethods.getVerbSpecLoc(segments,"VB", indexCor);
            boolean hasVerb = false;
            if (indexVerb == -1){
                indexVerb = getPosVerbFromSpecLoc(segments, "VBD", indexCor);
                verb = PosMethods.getVerbSpecLoc(segments,"VBD", indexCor);
                if(verb.trim().length() > 0){
                    hasVerb = true;
                }
                if(!hasVerb){
                    indexVerb = getPosVerbFromSpecLoc(segments, "VBP", indexCor);
                    verb = PosMethods.getVerbSpecLoc(segments,"VBP", indexCor);
                    if(verb.trim().length() > 0){
                        hasVerb = true;
                    }
                }
            }

        }
        Sresp sresp = new Sresp(false,false,false);
        sresp.setsRespTagged(segments);

        //Scenario 0: First bullet point: IFSIG must.
        if(indexSubConj==-1 && indexVerb == -1 && indexModVerb ==-1 && indexNoun != -1 ){
            //actor
            sresp.setActor(true);
        }else if(indexSubConj==-1 && indexVerb == -1 && indexModVerb !=-1 && indexNoun != -1 ){
            // Actor modalVerb
            if(indexNoun < indexModVerb){
                sresp.setActor(true);
                sresp.setModalVerb(true);
                sresp.setVerb(false);
            }
        }else if(indexSubConj==-1 && indexModVerb !=-1 && indexNoun != -1 && indexVerb != -1){
            //Actor modalVerb Verb
            if(indexNoun < indexModVerb && indexModVerb < indexVerb){
                sresp.setActor(true);
                sresp.setModalVerb(true);
                sresp.setVerb(true);

                if(isPassiveVoiceSr(segments)){
                    sresp.setpVoice(true);
                    String mainVb = PosMethods.getVerbPassiveVoiceSr(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        sresp.setAmbVerb(true);
                    }
                }else{
                    String stemmedVerb = Utils.wordStemmer(verb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        sresp.setAmbVerb(true);
                    }
                }
                isSRBullet = true;
            }// must create something
            else if(indexModVerb <  indexVerb && indexVerb < indexNoun){

                sresp.setActor(false);
                sresp.setModalVerb(true);
                sresp.setVerb(true);
                if(isPassiveVoiceSr(segments)){
                    sresp.setpVoice(true);
                    String mainVb = PosMethods.getVerbPassiveVoiceSr(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        sresp.setAmbVerb(true);
                    }
                }else{
                    String stemmedVerb = Utils.wordStemmer(verb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        sresp.setAmbVerb(true);
                    }
                }

                isSRBullet = true;
            }
            //Noun and verb
        }else if(indexSubConj==-1 && indexModVerb ==-1 && indexNoun != -1 && indexVerb != -1){
            //  Actor creates something
            if(indexNoun < indexVerb ){

                sresp.setActor(true);
                sresp.setModalVerb(false);
                sresp.setVerb(true);
                if(isPassiveVoiceSr(segments)){
                    sresp.setpVoice(true);
                    String mainVb = PosMethods.getVerbPassiveVoiceSr(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        sresp.setAmbVerb(true);
                    }
                }else{
                    String stemmedVerb = Utils.wordStemmer(verb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        sresp.setAmbVerb(true);
                    }
                }
            }else if(indexVerb < indexNoun ){
                //Scenario 3: Create something

                sresp.setActor(false);
                sresp.setModalVerb(false);
                sresp.setVerb(true);

                if(isPassiveVoiceSr(segments)){
                    sresp.setpVoice(true);
                    String mainVb = PosMethods.getVerbPassiveVoiceSr(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        sresp.setAmbVerb(true);
                    }
                }else{
                    String stemmedVerb = Utils.wordStemmer(verb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        sresp.setAmbVerb(true);
                    }
                }
            }
        }else {
            sresp.setNotMatched(true);
        }

        return  sresp;
    }

    /*
    * Method that checks if a multiple system response (2 or more system responses) contain smells such as not
    * precise verb, passive voice
    * */
    public static String getTypeMultSresp(ArrayList<Sresp> resps){

        String typeMultSr = "";
        int completeSresp = 0;
        int completeSrespNoActor =0;
        int incomplCondNoModalNoVerb =0;
        int incomplCondNoVerb = 0;
        int incomplCondNoActorNoModal =0;
        int incomplCondNoModal = 0; // Sc5 A send b


        int noActor = 0;
        int noModal = 0;
        int noVerb = 0;
        int pVoice = 0;
        int ambVerb = 0;



        for(Sresp sr:resps){


            if(sr.getActor() && sr.getModalVerb() && sr.getVerb()){
                completeSresp++;
                if(sr.getpVoice()){
                    pVoice++;
                }
                if(sr.getAmbVerb()){
                    ambVerb++;
                }
            }else if(!sr.getActor() && sr.getModalVerb() && sr.getVerb()){
                completeSrespNoActor++;
                noActor++;
                if(sr.getpVoice()){
                    pVoice++;
                }
                if(sr.getAmbVerb()){
                    ambVerb++;
                }
            }else if(sr.getActor() && !sr.getModalVerb() && !sr.getVerb()){
                incomplCondNoModalNoVerb++;
                noModal++;
                noVerb++;
            }else if(sr.getActor() && sr.getModalVerb() && !sr.getVerb()){
                incomplCondNoVerb++;
                noVerb++;
            }else if(sr.getActor() && !sr.getModalVerb() && sr.getVerb()){
                incomplCondNoModal++;
                noModal++;
                if(sr.getpVoice()){
                    pVoice++;
                }
                if(sr.getAmbVerb()){
                    ambVerb++;
                }
            }else if(!sr.getActor() && !sr.getModalVerb() && sr.getVerb()){
                incomplCondNoActorNoModal++;
                noActor++;
                noModal++;
                if(sr.getpVoice()){
                    pVoice++;
                }
                if(sr.getAmbVerb()){
                    ambVerb++;
                }
            }
        }

        if(completeSresp>=2){
            typeMultSr = "multSrsPos";
            if(pVoice>0){
                typeMultSr += "WithPassiveSR";
            }if(ambVerb>0){
                typeMultSr += "WithAmbVerb";
            }

        }else if(completeSresp>=1 && completeSrespNoActor>=1){
            typeMultSr = "multSrsPos";
            if(pVoice>0){
                typeMultSr += "WithPassiveSR";
            }if(ambVerb>0){
                typeMultSr += "WithAmbVerb";
            }

        }else if(completeSresp>=1 && incomplCondNoActorNoModal >= 1){
            typeMultSr = "multSrsPos";
            if(pVoice>0){
                typeMultSr += "WithPassiveSR";
            }if(ambVerb>0){
                typeMultSr += "WithAmbVerb";
            }
        }else if(incomplCondNoModalNoVerb>=1 && completeSrespNoActor >=2 ){
            typeMultSr = "multSrsPos";
            if(pVoice>0){
                typeMultSr += "WithPassiveSR";
            }if(ambVerb>0){
                typeMultSr += "WithAmbVerb";
            }
        }else if(incomplCondNoModalNoVerb>=1 && completeSrespNoActor >=1 && incomplCondNoActorNoModal>=1 ){
            typeMultSr = "multSrsPos";
            if(pVoice>0){
                typeMultSr += "WithPassiveSR";
            }if(ambVerb>0){
                typeMultSr += "WithAmbVerb";
            }
        }else if(incomplCondNoVerb>=1 && incomplCondNoActorNoModal >=2){
            typeMultSr = "multSrsPos";
            if(pVoice>0){
                typeMultSr += "WithPassiveSR";
            }if(ambVerb>0){
                typeMultSr += "WithAmbVerb";
            }

        }else if(incomplCondNoVerb>=1 && incomplCondNoModal>=2){
            typeMultSr = "multSrsPos";
            if(pVoice>0){
                typeMultSr += "WithPassiveSR";
            }if(ambVerb>0){
                typeMultSr += "WithAmbVerb";
            }

        }
        return typeMultSr;
    }

}
