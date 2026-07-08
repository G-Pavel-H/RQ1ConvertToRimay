import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

import javafx.util.Pair;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    /*
    * Match a tregex pattern
    * */
    public static  Boolean patternMatchInReq(String reqParsingTree, String pattern)
    {
        Boolean matchInReq =  false;
        TregexPattern p = TregexPattern.compile(pattern);
        Tree treeTest = Tree.valueOf(reqParsingTree);
        TregexMatcher matchReq = p.matcher(treeTest);
        if(matchReq.find())
        {
            matchInReq = true;
        }
        return  matchInReq;
    }

    public static Pair<Integer,Boolean> isClauseInList(List<MatchPattern> listClausesInReq, String clause){
        Boolean contains = false;
        int indexContains = -1;

        for( int i =0 ; i< listClausesInReq.size();  i++)
        {
            if(listClausesInReq.get(i).getTextMatch().trim().equals((clause.trim())))
            {
                contains = true;
                indexContains = i; //the position in the array
            }
        }
        Pair<Integer, Boolean> containsIndex = new Pair<Integer, Boolean>(indexContains,contains);

        return containsIndex;
    }

    //get the segments of a requirement
    public static List<SegmentReq> getIndexesClausesInReq(List<MatchPattern> segmentsReq, String r, MaxentTagger tagger, String idReq)
    {
        List<String> wordsReqOrig = new ArrayList<String>();
        String reqOriginal = PosMethods.removeNoBreakCharacter(r);
        reqOriginal = PosMethods.removeBlankSpaces(reqOriginal);


        Collections.addAll(wordsReqOrig, reqOriginal.split(" "));
        List<SegmentReq> segmentsInReq = new ArrayList<SegmentReq>();

        Integer startSegmentReq = -1;
        Integer endSegmentReq = -1;

        for (MatchPattern seg : segmentsReq)
        {


            String clauseType = seg.getTypeOfPattern();
            boolean isMatch = false;
            //Just for considering the notMatched
            if(clauseType!="NotMatch"){
                isMatch = true;
            }
            String passiveVoice = seg.getPassiveVoice();
            String passiveVoiceSR = seg.getPassiveVoiceSR();
            String verbCondConcept = "";
            String verbSr = "";
            for(TupleConcept t: seg.getConceptsInText()){
                if(t.getNameConcept().toLowerCase().contains("verbcondition")){
                    verbCondConcept = t.getContentConcept();
                }
                if(t.getNameConcept().toLowerCase().contains("verb")){
                    verbSr = t.getContentConcept();
                }
            }
            String verbCondition = seg.getVerCondition();

            if(verbCondition == null){
                verbCondition = verbCondConcept;
            }else if(verbCondition.trim().length() == 0){
                verbCondition = verbCondConcept;
            }

            if(passiveVoice != null && verbCondition == null){
                if(passiveVoice.toLowerCase().contains("passive")){
                    verbCondition = PosMethods.getVerbPassiveVoice( PosMethods.getTagSentence(seg.getTextMatch(),tagger));
                }
            }
            String[] wordsSegment = seg.getTextMatch().split(" ");
            if(wordsSegment.length >= 1)
            {
                List<Integer> indexesWordSegment = getAllIndexOcurrences(reqOriginal , wordsSegment[0]);
                if(indexesWordSegment.size() >= 1)
                {
                    for(int indexWord : indexesWordSegment)
                    {

                        int counter = 0;
                        int indexAndSegLen = indexWord+wordsSegment.length;

                        if(indexAndSegLen <= wordsReqOrig.size() ) {
                            for (int j = indexWord; j < (indexWord + wordsSegment.length); j++) {

                                if ((wordsSegment[counter].toLowerCase()).equals((wordsReqOrig.get(j).toLowerCase()))
                                        || (wordsReqOrig.get(j).toLowerCase()).contains((wordsSegment[counter]).toLowerCase())) {

                                    if (j == ((indexWord + wordsSegment.length) - 1)) {
                                        startSegmentReq = indexWord;
                                        endSegmentReq = j;
                                    }
                                    counter += 1;

                                } else {
                                    break;
                                }
                            }
                        }



                    }
                    if(!startSegmentReq.equals(-1) && !endSegmentReq.equals(-1) )
                    {


                        SegmentReq segmentInReq = new SegmentReq(startSegmentReq,endSegmentReq, isMatch, clauseType, passiveVoice, passiveVoiceSR);
                        segmentInReq.setVerbCondition(verbCondition);
                        segmentInReq.setSrVerb(verbSr);



                        if(seg.getConceptsInText().size()>0)
                        {
                            segmentInReq.setConceptsInText(seg.getConceptsInText());

                            //Assigning the concepts to the variables
                            segmentInReq.mapConcepts(seg.getConceptsInText());

                            if(passiveVoice != null){
                                if(passiveVoice.toLowerCase().contains("passive")){

                                    if(segmentInReq.getVerbCondition()!=null){
                                        if(segmentInReq.getVerbCondition().trim().length() == 0){
                                            segmentInReq.setVerbCondition(verbCondition);
                                        }
                                    }

                                }
                            }

                        }

                        segmentsInReq.add(segmentInReq);

                    }

                }
            }
        }

        return segmentsInReq;
    }


    public static List<Integer> getAllIndexOcurrences(String requirement, String wordToSearch)
    {
        String[] wordsInReq = requirement.split(" ");
        List<Integer> ocurrencesWord = new ArrayList<>();
        for (int i=0; i <  wordsInReq.length; i++)
        {
            if(wordsInReq[i].contains(wordToSearch.trim()))
            {
                ocurrencesWord.add(i);
            }
        }
        return ocurrencesWord;
    }


    public static boolean overlapLeftSegment(SegmentReq a, SegmentReq b)
    {
        boolean isOverlapLeft = false;
        if (b.getStartSegment() < a.getStartSegment() && b.getEndSegment() > a.getStartSegment() &&
                b.getEndSegment() <= a.getEndSegment())
        {
            isOverlapLeft = true;
        }
        return isOverlapLeft;
    }


    public static SegmentReq mergeOverlapLeftSegment(SegmentReq a, SegmentReq b)
    {
        SegmentReq newRangeOverLeftSegment = new SegmentReq( b.getStartSegment(), a.getEndSegment(),
                a.isMatched(), a.mergeTypeOfClause2(b.getTypeOfClause()), a.mergePassiveVCond(b.getPassiveVoiceCond()),
                a.mergePassiveVSR(b.getPassiveVoiceSR()));
        if(a.getVerbCondition()!=null){
            if(a.getVerbCondition().trim().length()>0){
                newRangeOverLeftSegment.setVerbCondition(a.mergeVerbCond(b.getVerbCondition()));
            }
        }
        //Concepts:
        if(b.getConceptsInText().size() > 0) {
            newRangeOverLeftSegment.setConceptsInText(a.mergeConcepts2(b.getConceptsInText()));//setConceptsInText(List<TupleConcept> concepts
            //Mapping the concepts
            newRangeOverLeftSegment.mapConcepts(a.mergeConcepts2(b.getConceptsInText()));
        }else
        {
            newRangeOverLeftSegment.setConceptsInText(a.getConceptsInText());
            //Mapping the concepts
            newRangeOverLeftSegment.mapConcepts(a.getConceptsInText());
        }
        return newRangeOverLeftSegment;
    }

    public static boolean overlapRigthSegment(SegmentReq a, SegmentReq b)
    {
        boolean isOverlapRigth = false;
        if(b.getStartSegment() >= a.getStartSegment() && b.getStartSegment() < a.getEndSegment() && b.getEndSegment() >a.getEndSegment())
        {
            isOverlapRigth = true;
        }
        return isOverlapRigth;
    }

    public static SegmentReq mergeOverlapRigthSegment(SegmentReq a, SegmentReq b)
    {
        SegmentReq newRangeOverRigthSegment = new SegmentReq(a.getStartSegment(),b.getEndSegment(), a.isMatched(),
                a.mergeTypeOfClause2(b.getTypeOfClause()), a.mergePassiveVCond(b.getPassiveVoiceCond()),
                a.mergePassiveVSR(b.getPassiveVoiceSR()));

        if(a.getVerbCondition()!=null){
            if(a.getVerbCondition().trim().length()>0){
                newRangeOverRigthSegment.setVerbCondition(a.mergeVerbCond(b.getVerbCondition()));
            }
        }

        if(b.getConceptsInText().size() > 0) {
            newRangeOverRigthSegment.setConceptsInText(a.mergeConcepts2(b.getConceptsInText()));//setConceptsInText(List<TupleConcept> concepts
            newRangeOverRigthSegment.mapConcepts(a.mergeConcepts2(b.getConceptsInText()));
        }else
        {
            newRangeOverRigthSegment.setConceptsInText(a.getConceptsInText());
            newRangeOverRigthSegment.mapConcepts(a.getConceptsInText());
        }
        return newRangeOverRigthSegment;
    }

    public static boolean aContainsbSegment(SegmentReq a, SegmentReq b)
    {
        boolean doesAContainsB = false;
        if(b.getStartSegment() >= a.getStartSegment() && b.getEndSegment() <= a.getEndSegment())
        {
            doesAContainsB = true;
        }
        return  doesAContainsB;
    }

    public static SegmentReq mergeAcontainsBSegment(SegmentReq a, SegmentReq b)
    {
        SegmentReq newRangeAcontaisBSegment = new SegmentReq(a.getStartSegment(), a.getEndSegment(),a.isMatched(),
                a.mergeTypeOfClause2(b.getTypeOfClause()), a.mergePassiveVCond(b.getPassiveVoiceCond()),
                a.mergePassiveVSR(b.getPassiveVoiceSR()));
        if(a.getVerbCondition()!=null){
            if(a.getVerbCondition().trim().length()>0){
                newRangeAcontaisBSegment.setVerbCondition(a.mergeVerbCond(b.getVerbCondition()));
            }
        }

        if(b.getConceptsInText().size() > 0) {
            newRangeAcontaisBSegment.setConceptsInText(a.mergeConcepts2(b.getConceptsInText()));//setConceptsInText(List<TupleConcept> concepts
            newRangeAcontaisBSegment.mapConcepts(a.mergeConcepts2(b.getConceptsInText()));
        }else
        {
            newRangeAcontaisBSegment.setConceptsInText(a.getConceptsInText());
            newRangeAcontaisBSegment.mapConcepts(a.getConceptsInText());
        }
        return  newRangeAcontaisBSegment;
    }

    public static boolean bContainsaSegment(SegmentReq a, SegmentReq b)
    {
        boolean doesBContainsA = false;
        if(a.getStartSegment()>= b.getStartSegment() && a.getEndSegment() <= b.getEndSegment())
        {
            doesBContainsA = true;
        }
        return  doesBContainsA;
    }

    public static SegmentReq mergeBcontainsASegment(SegmentReq a, SegmentReq b)
    {
        SegmentReq newRangeBcontainsA = new SegmentReq(b.getStartSegment(), b.getEndSegment(), b.isMatched(),
                b.mergeTypeOfClause2(a.getTypeOfClause()), b.mergePassiveVCond(a.getPassiveVoiceCond()),
                b.mergePassiveVSR(a.getPassiveVoiceSR()));
        if(a.getVerbCondition()!=null){
            if(a.getVerbCondition().trim().length()>0){
                newRangeBcontainsA.setVerbCondition(a.mergeVerbCond(b.getVerbCondition()));
            }
        }
        if(a.getConceptsInText().size() > 0) {
            newRangeBcontainsA.setConceptsInText(b.mergeConcepts2(a.getConceptsInText()));//setConceptsInText(List<TupleConcept> concepts
            newRangeBcontainsA.mapConcepts(b.mergeConcepts2(a.getConceptsInText()));
        }else
        {
            newRangeBcontainsA.setConceptsInText(b.getConceptsInText());
            newRangeBcontainsA.mapConcepts(b.getConceptsInText());

        }
        return newRangeBcontainsA;
    }


    public static List<SegmentReq> mergeOverlapClausesNew (List<SegmentReq> segmentsInReq)
    {
        List<SegmentReq> segmentReqsUniques = new ArrayList<>();
        for (SegmentReq actual:segmentsInReq)
        {
            if (segmentReqsUniques.size() < 1)
            {
                segmentReqsUniques.add(actual);
            }else if(segmentReqsUniques.size() >= 1)
            {
                boolean matchInUniques = false;
                for (int i=0; i< segmentReqsUniques.size(); i++)
                {
                    if(overlapLeftSegment(actual, segmentReqsUniques.get(i)))
                    {
                        segmentReqsUniques.set(i, mergeOverlapLeftSegment(actual,segmentReqsUniques.get(i)));
                        matchInUniques = true;

                    }else if(overlapRigthSegment(actual, segmentReqsUniques.get(i)))
                    {
                        segmentReqsUniques.set(i, mergeOverlapRigthSegment(actual, segmentReqsUniques.get(i)));
                        matchInUniques = true;

                    }else if(aContainsbSegment(actual, segmentReqsUniques.get(i)))
                    {
                        segmentReqsUniques.set(i, mergeAcontainsBSegment(actual, segmentReqsUniques.get(i)));
                        matchInUniques = true;

                    }else if(bContainsaSegment(actual, segmentReqsUniques.get(i)))
                    {
                        segmentReqsUniques.set(i, mergeBcontainsASegment(actual, segmentReqsUniques.get(i)));
                        matchInUniques = true;
                    }
                }
                if(!matchInUniques)
                {

                    segmentReqsUniques.add(actual);
                }
            }
        }
        return segmentReqsUniques;
    }

    public static ArrayList<String> getVerbsReq(Requirement singleReq, MaxentTagger tagger, String id)
    {

        ArrayList<String> listConditions = new ArrayList<String>();
        for (SegmentReq clauseInReq: singleReq.getOrderSegs())
        {
            if(clauseInReq.getTypeOfClause().contains("cond") &&
                    !clauseInReq.getTypeOfClause().contains("conditions12")){
                if (clauseInReq.getVerbCondition() != null )
                {
                    if(!clauseInReq.getTypeOfClause().toLowerCase().contains("conditionwithsymbol")){
                        String verbInCond = clauseInReq.getVerbCondition();

                        if(verbInCond.toLowerCase().contains("is") || verbInCond.toLowerCase().contains("are")||
                                verbInCond.toLowerCase().contains("has") || verbInCond.toLowerCase().contains("have")){
                            String conditionTemp = Utils.getStringFromSegment(clauseInReq.getStartSegment(), clauseInReq.getEndSegment(),singleReq.getPreprocessedReq());
                            ArrayList<TaggedWord> tagCond = PosMethods.getTagSentence(conditionTemp, tagger);

                            for(int i =0; i< tagCond.size(); i++ ){

                                if (tagCond.get(i).word().trim().toLowerCase().equals(verbInCond.trim().toLowerCase())){
                                    if (i+1 < tagCond.size()){
                                        if(tagCond.get(i+1).tag().toLowerCase().contains("vb")){
                                            verbInCond = tagCond.get(i+1).word();

                                        }
                                    }
                                }

                            }

                        }
                        String pennTreeVerb = clauseInReq.getPennTreeBankVerbCondition();
                        String typeCond = PosMethods.getTypeConditionCsv(verbInCond, pennTreeVerb);
                        listConditions.add(typeCond);
                    }else{

                        listConditions.add("logicalExpression");
                    }

                }else{
                    if(clauseInReq.getTypeOfClause().toLowerCase().contains("conditionwithsymbol")){

                        listConditions.add("logicalExpression");
                    }else{
                        String conditionTemp = Utils.getStringFromSegment(clauseInReq.getStartSegment(), clauseInReq.getEndSegment(),singleReq.getPreprocessedReq());
                        ArrayList<TaggedWord> tagCond = PosMethods.getTagSentence(conditionTemp, tagger);
                        int posVerb = PosMethodsCondition.getVerbInCond(tagCond);
                        String pennTreeVerb = clauseInReq.getPennTreeBankVerbCondition();
                        if(posVerb!=-1){

                            String verbAux = tagCond.get(posVerb).word();
                            if(verbAux.toLowerCase().contains("is") || verbAux.toLowerCase().contains("are")||
                                    verbAux.toLowerCase().contains("has") || verbAux.toLowerCase().contains("have")){

                                if(posVerb +1 <= tagCond.size()){
                                    if(tagCond.get(posVerb+1).tag().toLowerCase().contains("vb")){
                                        verbAux = tagCond.get(posVerb+1).word();
                                    }
                                }

                            }
                            String typeCond = PosMethods.getTypeConditionCsv(verbAux, pennTreeVerb);
                            listConditions.add(typeCond);
                        }
                    }

                }

            }

        }
        return  listConditions;
    }

    public static ArrayList<SegmentReq> getCondTimes(Requirement r){
        ArrayList<SegmentReq> timeConds = new ArrayList<>();
        for(SegmentReq s:r.getOrderSegs()){
            if(s.getTypeOfClause().toLowerCase().contains("conditions12")){
                timeConds.add(s);
            }
        }
        return timeConds;
    }

    public static ArrayList<String> getScopeReq(Requirement r){
        ArrayList<String> scopes = new ArrayList<>();
        for(SegmentReq s:r.getOrderSegs()){
            if(s.getTypeOfClause().toLowerCase().contains("scope")){
                scopes.add(s.getTypeOfClause().toLowerCase());
            }
        }
        return scopes;
    }

    public static String getStringFromSegment(int start, int end, String requirement)
    {
        String sequence = "";
        String[] wordInReq = requirement.split(" ");
        for(int i = start; i <= end; i++ )
        {
            sequence += wordInReq[i]+" ";

        }
        return sequence.trim();
    }


    public static ArrayList<String> getRealListValues(ArrayList<String> typeConditions)
    {
        ArrayList<String> newList = new ArrayList<>();
        for (String element:typeConditions){
            if(element.trim().length() > 1){
                newList.add(element);
            }

        }
        return newList;
    }


    public static String getTypePatternRimayCsv(String typeRequirement, ArrayList<String> typeConditions, ArrayList<String> scopes, String id, ArrayList<SegmentReq> timeConds) {

        ArrayList<String> listCondsNew = getRealListValues(typeConditions);
        String rimayPattern = "";
        switch (typeRequirement.toLowerCase())
        {
            case "sysresp":
                rimayPattern = "5. System response";
                break;
            case "multsrs":
                rimayPattern = "5. System response";
                break;
            case "scopesysresp":
                rimayPattern = "1. Scope and SR";
                break;
            case "scopemultsrs":
                rimayPattern = "1. Scope and SR";
                break;
            case "multcondsysresp":
                rimayPattern = "10. Multiple conditions, and SR";
                break;
            case "multcondmultsrs":
                rimayPattern = "10. Multiple conditions, and SR";
                break;
            case "scopemultcondsysresp":
                rimayPattern = "9. Scope, multiple conditions, and SR";
                break;
            case "scopemultcondmultsrs":
                rimayPattern = "9. Scope, multiple conditions, and SR";
                break;
            case "condsysresp": //#2
                if(listCondsNew.size() == 1)
                {
                    String typeCondition = listCondsNew.get(0);
                    if(typeCondition.equals("trigger"))
                    {
                        rimayPattern = "7. Condition(Trigger), and SR";

                    }else if(typeCondition.equals("logicalExpression"))
                    {
                        rimayPattern = "6. Condition(Precondition LE), and SR";

                    }else
                    {
                        rimayPattern = "11. Others";
                    }
                }else if(timeConds.size() ==1){
                    rimayPattern = "8. Condition (Time Adverb) and SR";
                }
                break;
            case "condmultsrs":
                if(listCondsNew.size() == 1)
                {
                    String typeCondition = listCondsNew.get(0);
                    if(typeCondition.equals("trigger"))
                    {
                        rimayPattern = "7. Condition(Trigger), and SR";

                    }else if(typeCondition.equals("logicalExpression"))
                    {
                        rimayPattern = "6. Condition(Precondition LE), and SR";

                    }else
                    {
                        rimayPattern = "11. Others";
                    }
                }else if(timeConds.size() ==1){
                    rimayPattern = "8. Condition (Time Adverb) and SR";
                }
                break;

            case "scopecondsysresp":
                if(listCondsNew.size()==1)
                {
                    String typeCondition = listCondsNew.get(0);
                    if(typeCondition.equals("trigger"))
                    {
                        rimayPattern = "3. Scope, condition(Trigger), and SR";
                    }else if(typeCondition.equals("logicalExpression"))
                    {
                        rimayPattern = "2. Scope, condition(Precondition LE), and SR";
                    }else
                    {
                        rimayPattern = "11. Others";
                    }
                }else if(timeConds.size()==1){
                    rimayPattern = "4. Scope, condition (Time Adverb) and SR";
                }
                break;
            case "scopecondmultsrs":
                if(listCondsNew.size()==1)
                {
                    String typeCondition = listCondsNew.get(0);
                    if(typeCondition.equals("trigger"))
                    {
                        rimayPattern = "3. Scope, condition(Trigger), and SR";
                    }else if(typeCondition.equals("logicalExpression"))
                    {
                        rimayPattern = "2. Scope, condition(Precondition LE), and SR";
                    }else
                    {
                        rimayPattern = "11. Others";
                    }
                }else if(timeConds.size()==1){
                    rimayPattern = "4. Scope, condition (Time Adverb) and SR";
                }
                break;
            case "othertype":
                Boolean hasScope = false;
                if(scopes.size()>0){
                    hasScope = true;
                }
                if(listCondsNew.size() == 1)
                {
                    String typeCondition = listCondsNew.get(0);

                    if(typeCondition.equals("trigger") && !hasScope)
                    {
                        rimayPattern = "7. Condition(Trigger), and SR";

                    }else if(typeCondition.equals("logicalExpression") && !hasScope )
                    {
                        rimayPattern = "6. Condition(Precondition LE), and SR";

                    }else if (typeCondition.equals("trigger") && hasScope){
                        rimayPattern = "3. Scope, condition(Trigger), and SR";
                    }else if(typeCondition.equals("logicalExpression") && hasScope ){
                        rimayPattern = "2. Scope, condition(Precondition LE), and SR";
                    }
                    else
                    {
                        rimayPattern = "11. Others";
                    }
                }else if(timeConds.size() ==1){
                    rimayPattern = "8. Condition (Time Adverb) and SR";
                }else if(listCondsNew.size() > 1){
                    if(hasScope){
                        rimayPattern = "9. Scope, multiple conditions, and SR";
                    }else{
                        rimayPattern = "10. Multiple conditions, and SR";
                    }
                }else if(hasScope){
                    rimayPattern = "1. Scope and SR";
                }else{
                    rimayPattern = "5. System response";
                }
        }
        return rimayPattern;
    }


    public static List<SegmentReq> removeDuplicatesClausesInReq(List<SegmentReq> segmentsInReq, MaxentTagger tagger, String reqID, String preReq)
    {
        List<SegmentReq> noDuplicatesSegments = new ArrayList<>();

        for(SegmentReq segment: segmentsInReq)
        {
            if(!existInList(noDuplicatesSegments, segment.getStartSegment(), segment.getEndSegment()))
            {

                SegmentReq newSeg = new SegmentReq(segment.getStartSegment(), segment.getEndSegment(),true, segment.getTypeOfClause(), segment.getPassiveVoiceCond(), segment.getPassiveVoiceSR());
                newSeg.setVerbCondition(segment.getVerbCondition());
                if(segment.getConceptsInText().size() > 0)
                {
                    newSeg.setConceptsInText(segment.getConceptsInText());
                    newSeg.mapConcepts(segment.getConceptsInText());

                    if(newSeg.getPassiveVoiceCond()!=null && newSeg.getVerbCondition() == null ){

                        if(newSeg.getPassiveVoiceCond().toLowerCase().contains("passive")){
                            if(segment.getVerbCondition()!=null){
                                if(segment.getVerbCondition().trim().length() > 0){
                                    newSeg.setVerbCondition(segment.getVerbCondition());
                                }else {
                                    String verbCond = PosMethods.getVerbPassiveVoice( PosMethods.getTagSentence(Utils.getStringFromSegment(newSeg.getStartSegment(),newSeg.getEndSegment(),preReq),tagger));
                                    newSeg.setVerbCondition(verbCond);
                                }
                            }else{
                                String verbCond = PosMethods.getVerbPassiveVoice( PosMethods.getTagSentence(Utils.getStringFromSegment(newSeg.getStartSegment(),newSeg.getEndSegment(),preReq),tagger));
                                newSeg.setVerbCondition(verbCond);
                            }
                        }
                    }
                }
                noDuplicatesSegments.add(newSeg);
            }
        }
        return noDuplicatesSegments;
    }



    public static Boolean existInList(List<SegmentReq> segments, int start, int end)
    {
        Boolean isElementList = false;
        for(SegmentReq seg : segments)
        {
            if(start == seg.getStartSegment() && end == seg.getEndSegment())
            {
                isElementList = true;
            }
        }
        return isElementList;
    }


    public static  List<SegmentReq> getNotMatchSegmentsInReq(List<SegmentReq> clausesInReq, String requirement)
    {
        int length = 1;
        int start = -1;
        //Get the index of the word (in the req) plus a label whether it was matched or not by the pattern
        HashMap<Integer, String> indexWordMatch = labelMatchWords(clausesInReq, requirement);
        //From the Hash map gets the List of index that were not matched
        ArrayList<Integer> notMatchedSequence = getListSequenceNotMatch(indexWordMatch);
        List<SegmentReq> notMatchedReqs =  new ArrayList<SegmentReq>();


        for(int i = 1; i <= notMatchedSequence.size(); i++ )
        {
            if( i == notMatchedSequence.size() || ((notMatchedSequence.get(i) - notMatchedSequence.get(i-1)) != 1))
            {
                if(length == 1) // When the segment only contains a word
                {
                    SegmentReq notMatchedSeq = new SegmentReq(notMatchedSequence.get(i-length), notMatchedSequence.get(i-length),false, "", "","");
                    notMatchedReqs.add(notMatchedSeq);
                }else {  //when the segment contains a phrase or sentence
                    int segment_length = notMatchedSequence.get(i-1) - notMatchedSequence.get(i-length);
                    String typeClause = "";
                    if (segment_length > 2)
                    {
                        typeClause = "NotMatch";
                    }
                    SegmentReq notMatchedSeq = new SegmentReq(notMatchedSequence.get(i-length), notMatchedSequence.get(i-1), false, typeClause,"","");
                    notMatchedReqs.add(notMatchedSeq);
                }
                length = 1;

            }else
            {
                length += 1;
            }
        }
        return notMatchedReqs;
    }


    public static HashMap<Integer, String> labelMatchWords(List<SegmentReq> segmentsInReq, String requirement)
    {
        HashMap<Integer, String> indexWordMatch = new HashMap<Integer, String>();
        for(int i=0; i < (requirement.trim().split(" ").length); i++)
        {
            indexWordMatch.put(i, "not_matched");
        }

        for(SegmentReq segment: segmentsInReq)
        {
            int start = segment.getStartSegment();
            int end =  segment.getEndSegment();
            for (int j=start; j <= end; j++)
            {
                indexWordMatch.replace(j, "matched");
            }
        }
        return indexWordMatch;
    }

    public static Integer numberReqWithSmells(List<Requirement> requirements){
        int reqWithSmells = 0;

        for(Requirement r: requirements){
            boolean hasReqSmell = false;
            boolean hasSegSmell = false;
            SmellReq sr = r.getSmellsReq();

            if(sr.getNonAtomic()){
                hasReqSmell = true;
            }
            if(sr.getIncompReq()){
                hasReqSmell = true;
            }
            if(sr.getUnorderCondReq()){
                hasReqSmell = true;
            }
            if(sr.getNotRequirement()){
                hasReqSmell = true;
            }
            if(sr.getCoordAmbCommaOr()){
                hasReqSmell = true;
            }
            if(r.getOrderSegs() != null ){
                for(SegmentReq clause : r.getOrderSegs()){
                    SmellSegment errorSeg = clause.getErrorsSegment();
                    if(errorSeg.getNoActorCond()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getNoVerbCond()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getNoModalSR()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getNoVerbSR()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getNoNounScope()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getPVoiceCond()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getPVoiceSR()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getAmbVerbCond()){
                        hasSegSmell = true;
                    }
                    if(errorSeg.getAmbVerbSR()){
                        hasSegSmell = true;
                    }
                }

            }


            if(hasReqSmell || hasSegSmell){
                reqWithSmells++;
            }
        }
        return reqWithSmells;
    }


    public static ArrayList<Integer> getListSequenceNotMatch(HashMap<Integer, String> indexWordMatch)
    {
        ArrayList<Integer> notMatchedSequence = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : indexWordMatch.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            if(value.equals("not_matched"))
            {
                notMatchedSequence.add(key);
            }
        }
        return  notMatchedSequence;
    }

    //identify smells
    public static List<SegmentReq> identifySmellsSegsLevel(List<SegmentReq> orderSegs, String reqID, String cleanReq, MaxentTagger tagger)
    {
        for(SegmentReq s: orderSegs)
        {
            s = FindSmell.identifySmellSeg(s, reqID,cleanReq,tagger);
            SmellSegment errorSeg = s.getErrorsSegment();
            s.setErrorsSegment(errorSeg);
        }
        //We need to pass the whole requirement in order to apply the smell no modal in the SR
        //Applying smell no modal verb in the SR
        orderSegs = (PosMethods.noModVerbSR(orderSegs,tagger,cleanReq));
        return orderSegs;
    }


    //order the segments/clauses given a requirement
    public static List<SegmentReq> orderRanges(List<SegmentReq> rangeMatch, List<SegmentReq> rangeNotMatch)
    {

        List<SegmentReq> nonOrderedRanges = Stream.concat(rangeMatch.stream(), rangeNotMatch.stream()).collect(Collectors.toList());
        for(int i=0; i< nonOrderedRanges.size(); i++)
        {
            for(int j=1; j<nonOrderedRanges.size(); j++)
            {

                if(nonOrderedRanges.get(j).getStartSegment() < nonOrderedRanges.get(j-1).getStartSegment() )
                {
                    int aStart = nonOrderedRanges.get(j).getStartSegment();
                    int aEnd   = nonOrderedRanges.get(j).getEndSegment();
                    boolean aIsMatched = nonOrderedRanges.get(j).isMatched();

                    String aTypeClause = nonOrderedRanges.get(j).getTypeOfClause();
                    String aPassiveVoice = nonOrderedRanges.get(j).getPassiveVoiceCond();
                    String aPassiveVoiceSR = nonOrderedRanges.get(j).getPassiveVoiceSR();
                    String aVerbCondition = nonOrderedRanges.get(j).getVerbCondition();
                    List<TupleConcept> aConcepts = new ArrayList<TupleConcept>();
                    if(nonOrderedRanges.get(j).getConceptsInText().size() > 0)
                    {
                        aConcepts = nonOrderedRanges.get(j).getConceptsInText();
                    }


                    int bStart =  nonOrderedRanges.get(j-1).getStartSegment();
                    int bEnd = nonOrderedRanges.get(j-1).getEndSegment();
                    boolean bIsMatched = nonOrderedRanges.get(j-1).isMatched();
                    String bTypeClause = nonOrderedRanges.get(j-1).getTypeOfClause();
                    String bPassiveVoice = nonOrderedRanges.get(j-1).getPassiveVoiceCond();
                    String bPassiveVoiceSR = nonOrderedRanges.get(j-1).getPassiveVoiceSR();
                    String bVerbCondition = nonOrderedRanges.get(j-1).getVerbCondition();
                    List<TupleConcept> bConcepts = new ArrayList<TupleConcept>();
                    if(nonOrderedRanges.get(j-1).getConceptsInText().size() > 0)
                    {
                        bConcepts = nonOrderedRanges.get(j-1).getConceptsInText();
                    }

                    //Switch
                    nonOrderedRanges.get(j-1).setStartSegment(aStart);
                    nonOrderedRanges.get(j-1).setEndSegment(aEnd);
                    nonOrderedRanges.get(j-1).setIsMatched(aIsMatched);
                    nonOrderedRanges.get(j-1).setTypeOfClause(aTypeClause);
                    nonOrderedRanges.get(j-1).setConceptsInText(aConcepts);
                    nonOrderedRanges.get(j-1).mapConcepts(aConcepts);//Check
                    nonOrderedRanges.get(j-1).setPassiveVoiceCond(aPassiveVoice);
                    nonOrderedRanges.get(j-1).setPassiveVoiceSR(aPassiveVoiceSR);
                    nonOrderedRanges.get(j-1).setVerbCondition(aVerbCondition);

                    nonOrderedRanges.get(j).setStartSegment(bStart);
                    nonOrderedRanges.get(j).setEndSegment(bEnd);
                    nonOrderedRanges.get(j).setIsMatched(bIsMatched);
                    nonOrderedRanges.get(j).setTypeOfClause(bTypeClause);
                    nonOrderedRanges.get(j).setConceptsInText(bConcepts);
                    nonOrderedRanges.get(j).mapConcepts(bConcepts);//Check
                    nonOrderedRanges.get(j).setPassiveVoiceCond(bPassiveVoice);
                    nonOrderedRanges.get(j).setPassiveVoiceSR(bPassiveVoiceSR);
                    nonOrderedRanges.get(j).setVerbCondition(bVerbCondition);

                }
            }
        }
        return nonOrderedRanges;
    }

    //This method checks for multiple SR and multiple Conditions
    public static List<SegmentReq> matchMultCondSr(List<SegmentReq> clausesNoDupl, MaxentTagger tagger, String req, String reqId){

        for(SegmentReq seg: clausesNoDupl){
            String typeSeg = seg.getTypeOfClause();
            if(typeSeg.toLowerCase().contains("cond") && !typeSeg.toLowerCase().contains("mult")){
                String condPreproc = Utils.getStringFromSegment(seg.getStartSegment(),seg.getEndSegment(), req);
                ArrayList<TaggedWord> sTagged = PosMethods.getTagSentence(condPreproc, tagger);
                ArrayList<Condition> conds = PosMethodsCondition.detectMultCondReq(sTagged);
                String typeConds  = PosMethodsCondition.getTypeMultCondBullet(conds);
                boolean hasCoord = PosMethods.checkCoordAmbCond(conds,typeConds);

                if(typeConds.contains("mult")){
                    seg.setTypeOfClause(typeConds);
                    if(hasCoord){
                        seg.setTypeOfClause(typeConds+"CoordAmb");
                    }
                }
            }
            if(typeSeg.toLowerCase().contains("sysres") && !typeSeg.toLowerCase().contains("mult")){
                //Pre-processing for the bullet points
                String srPreproc = Utils.getStringFromSegment(seg.getStartSegment(),seg.getEndSegment(), req);
                ArrayList<TaggedWord> srTagged = PosMethods.getTagSentence(srPreproc, tagger);
                ArrayList<Sresp> sysResps = PosMethods.detectMultSysResponses(srTagged);
                String typeSresps = PosMethods.getTypeMultSresp(sysResps);

                if(typeSresps.contains("mult")){
                    seg.setTypeOfClause(typeSresps);
                }

            }

        }
        return clausesNoDupl;
    }

    //get extension of a file
    public static String getExtension(String fileName)
    {
        String[] wordsInName = fileName.split("\\.");
        String extension = wordsInName[ wordsInName.length -1];
        return extension;
    }

    //Apply the port stemmer to a string
    public static String wordStemmer(String word){
        PorterStemmer a = new PorterStemmer();
        return a.stem(word);
    }

    //Get constituency parsing trees from the csv document
    public static ArrayList<ArrayList<String>> getParsingTreesFromCsv(String filePath)
    {
        ArrayList<ArrayList<String>> parsingTrees = new ArrayList<ArrayList<String>>();
        try
        {
            String row = "";
            BufferedReader csvReader = new BufferedReader(new FileReader(filePath));
            while ( (row = csvReader.readLine()) != null)
            {
                ArrayList<String> reqTree = new ArrayList<String>();
                String[] rowReq = row.split("\";\"");
                String reqParsingTree = rowReq[2].trim();
                String reqID = rowReq[0].replaceAll("\"","");
                String originalReq = rowReq[1];
                String preprocessedReq = rowReq[3];
                reqTree.add(reqID);
                reqTree.add(reqParsingTree);
                reqTree.add(originalReq);
                reqTree.add(preprocessedReq);
                parsingTrees.add(reqTree);
            }
            csvReader.close();

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return  parsingTrees;
    }

    //Print the frequencies of the requirements given the number of words they have
    public static void printFrequencies(ArrayList<Integer> numberWordsDataSet)
    {
        Integer counter0_50 = 0;
        Integer counter51_100 = 0;
        Integer counter101_150 = 0;
        Integer counter151_more = 0;

        for(Integer words:numberWordsDataSet)
        {
            if(words > 1 && words <= 50)
            {
                counter0_50 ++;
            }else if(words >50 && words <=100)
            {
                counter51_100 ++;
            }else if(words >100 && words <= 150)
            {
                counter101_150 ++;
            }else if(words >150)
            {
                counter151_more ++;
            }
        }
    }


}
