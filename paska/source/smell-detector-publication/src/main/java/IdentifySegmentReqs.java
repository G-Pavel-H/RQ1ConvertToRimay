import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import javafx.util.Pair;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.util.*;
import java.util.HashMap;

/*
* This class contains methods that help the tool to identify the segments (scope, condition, and system response).
* */
public class IdentifySegmentReqs {

    /*
    * This method returns segments/clauses from the requirement.
    * To do so, this method applies a Tregex pattern (extractionPattern) to a requirement.
    * It analyzes the results considering different scenarios and concepts.
    * Depending on the Tregex pattern applies this method performs different operations
    * */

    public static ArrayList<MatchPattern> extractClausesFromReq(String reqParsingTree, String extractionPattern, String typeClause, MaxentTagger tagger, String reqId)
    {
        TregexPattern p = TregexPattern.compile(extractionPattern);
        Tree treeTest = Tree.valueOf(reqParsingTree);
        TregexMatcher matchSeg = p.matcher(treeTest);

        ArrayList<MatchPattern> listClausesInReq = new ArrayList<>();
        while (matchSeg.find()) {


            String clauseInReq = new String();
            //For extracting the different parts of Clauses
            List<TupleConcept> partsClause = new ArrayList<>();
            //NP=nounPhrase $+ (VP=verbPhraseCond
            //For extracting the concepts from the different clauses of the requirement
            List<String> conceptsToExtract = new ArrayList<>();
            conceptsToExtract.add("actorCondition");
            conceptsToExtract.add("nounPhraseCond");

//            conceptsToExtract.add("verbCondition");
            conceptsToExtract.add("verbPhraseCond");
            conceptsToExtract.add("verbCondInc");

            //System response
            conceptsToExtract.add("actor");
            conceptsToExtract.add("modal");

//            conceptsToExtract.add("verb");
            conceptsToExtract.add("verbPhrase"); //including modal verb
            conceptsToExtract.add("VerbPhraseSR"); //no modal verb
            //To store all the concepts
            List<TupleConcept> conceptsReq = new ArrayList<>();
            String passiveVoice = "";
            String passiveVoiceSR = "";
            String textPVoiceCond = "";
            String textPVoiceSR ="";

            //Only we store the first occurence of the verb either in the condition or in the SR
            for(String concept : conceptsToExtract)
            {

                if(matchSeg.getNode(concept) != null)
                {
                    Tree querySubTree = matchSeg.getNode(concept);
                    ArrayList<TaggedWord> taggedWordsTree = querySubTree.taggedYield();
                    String conceptTreeBank = querySubTree.pennString();
                    String conceptText = "";

                    for(TaggedWord w:taggedWordsTree){
                        conceptText += w.value()+" ";
                    }

                    if(concept.equals("actor") || concept.equals("actorCondition") || concept.equals("nounPhraseCond"))
                    {
                        if (conceptText.split(" ").length < 3 )
                        {
                            if((conceptText.split(" ")[0]).toLowerCase().matches("the|The|a|an|An") )
                            {

                                TupleConcept conceptInRequirement = new TupleConcept(concept, conceptText.trim());
                                conceptInRequirement.setPennTreeConcept(conceptTreeBank);
                                conceptsReq.add(conceptInRequirement);
                            }
                            else {
                                TupleConcept conceptInRequirement = new TupleConcept(concept, conceptText.trim());
                                conceptInRequirement.setPennTreeConcept(conceptTreeBank);
                                conceptsReq.add(conceptInRequirement);
                            }
                        }
                    }else if (concept.equals("verbPhraseCond"))
                    {
                        int verbsInVP = PosMethods.getNumbersVerbsClause(taggedWordsTree);
                        ArrayList<TaggedWord> posVP = PosMethods.getPosVPClause(verbsInVP,taggedWordsTree);
                        String verbIntent = PosMethods.getVerbIntent(posVP,verbsInVP,reqId);
                        TupleConcept conceptInRequirement = new TupleConcept("verbCondition", verbIntent.trim());
                        conceptInRequirement.setPennTreeConcept(conceptTreeBank);
                        conceptInRequirement.setPosConcept(posVP);
                        conceptsReq.add(conceptInRequirement);

                    }else if(concept.equals("VerbPhraseSR"))
                    {
                        int verbsInSR = PosMethods.getNumbersVerbsClause(taggedWordsTree);
                        ArrayList<TaggedWord> posSR = PosMethods.getPosVPClause(verbsInSR,taggedWordsTree);
                        String verbSR = PosMethods.getVerbSR(posSR);
                        TupleConcept conceptInRequirement = new TupleConcept("verb", verbSR);
                        conceptInRequirement.setPennTreeConcept(conceptTreeBank);
                        conceptInRequirement.setPosConcept(posSR);
                        conceptsReq.add(conceptInRequirement);

                    }else if (conceptText.split(" ").length == 1 ) // This applies for the other concepts
                    {
                        TupleConcept conceptInRequirement = new TupleConcept(concept, conceptText.trim());
                        conceptInRequirement.setPennTreeConcept(conceptTreeBank);
                        conceptsReq.add(conceptInRequirement);
                    }
                }
            }

            if(typeClause.contains("sysresp"))
            {

                HashMap<String, String> patternPasVoiceSR =  ExtractionPattern.loadPatternsPassiveVoiceSR();
                List<TupleConcept> partsClausePvSr = new ArrayList<>();

                ArrayList<String> partsInReq = new ArrayList<>();
                partsInReq.add("nounPhrase");
                partsInReq.add("verbPhrase");

                for(Map.Entry<String, String> pattern : patternPasVoiceSR.entrySet())
                {
                    if(Utils.patternMatchInReq(reqParsingTree, pattern.getValue()))
                    {
                        passiveVoiceSR = pattern.getKey();
                        TregexPattern pPVSeg = TregexPattern.compile(pattern.getValue());
                        TregexMatcher matchPVSeg = pPVSeg.matcher(treeTest);

                        while (matchPVSeg.find()) {
                            if (matchPVSeg.getNode("nounPhrase") != null && matchPVSeg.getNode("verbPhrase") != null){

                                for(String part:partsInReq){
                                    Tree querySubTreeSr = matchPVSeg.getNode(part);
                                    ArrayList<Label> wordsInSubTreeSr = querySubTreeSr.yield();
                                    String partClauseTextSr = "";
                                    for(Label a : wordsInSubTreeSr){
                                        partClauseTextSr+= " "+a.value();
                                    }
                                    TupleConcept partInClausePvSr = new TupleConcept(part, partClauseTextSr.trim());
                                    partsClausePvSr.add(partInClausePvSr);
                                }
                                textPVoiceSR = mergePartsClause(partsClausePvSr);
                            }
                        }
                    }
                }
            }


            //For the passive voice This section will return the text of the passive voice either on tuples or String
            if(typeClause.contains("condition"))
            {
                HashMap<String, String> patternsPassiveVoice = ExtractionPattern.loadPatternsPassiveVoice();
                List<TupleConcept> partsCondPv = new ArrayList<>();
                ArrayList<String> partsCondType1 = new ArrayList<>();
                ArrayList<String> partsCondType2 = new ArrayList<>();
                ArrayList<String> partsCondType3 = new ArrayList<>();
                //conditionS1, conditionS3, conditionS5 First combination:  conjunction + clause
                partsCondType1.add("conjunction");
                partsCondType1.add("clause");
                ////conditionS6: Second combination: conjunction + nounPhraseCond +verbPhraseCond
                partsCondType2.add("conjunction");
                partsCondType2.add("nounPhraseCond");
                partsCondType2.add("verbPhraseCond");
                //conditionS11: Third: conjunction+nounPhraseCond
                partsCondType3.add("conjunction");
                partsCondType3.add("nounPhraseCond");
                //Patterns for detecting passive voices in the sentences
                for(Map.Entry<String, String> pattern : patternsPassiveVoice.entrySet()) {


                    if(Utils.patternMatchInReq(reqParsingTree, pattern.getValue()))
                    {
                        passiveVoice = pattern.getKey();
                        //Once it matches a PVoice pattern then we have to compare strings whether it is from the condition
                        TregexPattern pVoice = TregexPattern.compile(pattern.getValue());
                        TregexMatcher matchPVoice = pVoice.matcher(treeTest);

                        while (matchPVoice.find()) {
                            if(typeClause == "conditionS1" || typeClause == "conditionS3" || typeClause == "conditionS5" ){
                                for(String part:partsCondType1){
                                    if (matchPVoice.getNode(part) != null){
                                        Tree querySubTreeSr = matchPVoice.getNode(part);
                                        ArrayList<Label> wordsInSubTreeSr = querySubTreeSr.yield();
                                        String textCond = "";
                                        for(Label a : wordsInSubTreeSr){
                                            textCond+= " "+a.value();
                                        }
                                        TupleConcept partInPvCond = new TupleConcept(part, textCond.trim());
                                        partsCondPv.add(partInPvCond);
                                    }
                                }
                                textPVoiceCond = mergePartsClause(partsCondPv);

                            }else if(typeClause == "conditionS6"){
                                for(String part:partsCondType2){
                                    if (matchPVoice.getNode(part) != null){
                                        Tree querySubTreeSr = matchPVoice.getNode(part);
                                        ArrayList<Label> wordsInSubTreeSr = querySubTreeSr.yield();
                                        String textCond = "";
                                        for(Label a : wordsInSubTreeSr){
                                            textCond+= " "+a.value();
                                        }
                                        TupleConcept partInPvCond = new TupleConcept(part, textCond.trim());
                                        partsCondPv.add(partInPvCond);

                                    }

                                }
                                textPVoiceCond = mergePartsClause(partsCondPv);

                            }else if(typeClause == "conditionS11"){
                                for(String part:partsCondType3){
                                    if (matchPVoice.getNode(part) != null){
                                        Tree querySubTreeSr = matchPVoice.getNode(part);
                                        ArrayList<Label> wordsInSubTreeSr = querySubTreeSr.yield();
                                        String textCond = "";
                                        for(Label a : wordsInSubTreeSr){
                                            textCond+= " "+a.value();
                                        }
                                        TupleConcept partInPvCond = new TupleConcept(part, textCond.trim());
                                        partsCondPv.add(partInPvCond);

                                    }
                                }
                                textPVoiceCond = mergePartsClause(partsCondPv);

                            }else if(typeClause == "conditionS2" || typeClause=="conditionS4" || typeClause=="conditionS7" ||
                                    typeClause =="conditionS8"|| typeClause == "conditionS9"||typeClause=="conditionS10"||
                                    typeClause=="conditionS12"){ //These conditions they do not require
                                ArrayList<Label> yieldCondPVElements = matchPVoice.getMatch().yield();
                                for(Label a : yieldCondPVElements)
                                {
                                    textPVoiceCond += " "+a.value();
                                }
                            }
                        }
                    }
                }
            }

            //Extracting the segment/clause (not from a subtree)
            if(typeClause == "conditionS1" || typeClause == "conditionS3" || typeClause == "conditionS5")
            {
                ArrayList<String> partsCondType1 = new ArrayList<>();
                partsCondType1.add("conjunction");
                partsCondType1.add("clause");

                for(String part : partsCondType1) {
                    if (matchSeg.getNode(part) != null)
                    {
                        Tree querySubTree = matchSeg.getNode(part);
                        ArrayList<Label> wordsInSubTree = querySubTree.yield();
                        String partClauseText = "";
                        for(Label a : wordsInSubTree)
                        {
                            partClauseText += a.value()+" ";
                        }
                        TupleConcept partInClause = new TupleConcept(part, partClauseText.trim());
                        partsClause.add(partInClause);
                    }
                }
                String mergedCond = mergePartsClause(partsClause);
                if(mergedCond.trim().length() > 1){
                    MatchPattern matchClauseInReq = new MatchPattern(mergedCond.trim(), typeClause);
                    //adding the concepts
                    matchClauseInReq.setConceptsInText(conceptsReq);
                    if(mergedCond.trim().equals(textPVoiceCond.trim())){
                        matchClauseInReq.setPassiveVoice(passiveVoice);
                    }else{
                        matchClauseInReq.setPassiveVoice("");
                    }
                    matchClauseInReq.setPassiveVoiceSR("");
                    listClausesInReq.add(matchClauseInReq);
                }
            }else if(typeClause == "conditionS6"){
                ArrayList<String> partsCondType2 = new ArrayList<>();
                partsCondType2.add("conjunction");
                partsCondType2.add("nounPhraseCond");
                partsCondType2.add("verbPhraseCond");
                for(String part : partsCondType2) {
                    if (matchSeg.getNode(part) != null){
                        Tree querySubTree = matchSeg.getNode(part);
                        ArrayList<Label> wordsInSubTree = querySubTree.yield();
                        String partClauseText = "";
                        for(Label a : wordsInSubTree)
                        {
                            partClauseText += a.value()+" ";
                        }
                        TupleConcept partInClause = new TupleConcept(part, partClauseText.trim());
                        partsClause.add(partInClause);

                    }
                }
                String mergedCond = mergePartsClause(partsClause);
                if(mergedCond.trim().length()>1){
                    MatchPattern matchClauseInReq = new MatchPattern(mergedCond.trim(), typeClause);
                    //adding the concepts
                    matchClauseInReq.setConceptsInText(conceptsReq);
                    if(mergedCond.trim().equals(textPVoiceCond.trim())){
                        matchClauseInReq.setPassiveVoice(passiveVoice);
                    }else{
                        matchClauseInReq.setPassiveVoice("");
                    }
                    matchClauseInReq.setPassiveVoiceSR("");
                    listClausesInReq.add(matchClauseInReq);
                }
            }else if(typeClause == "conditionS11" ){
                ArrayList<String> partsCondType3 = new ArrayList<>();
                partsCondType3.add("conjunction");
                partsCondType3.add("nounPhraseCond");
                for(String part : partsCondType3) {
                    if (matchSeg.getNode(part) != null){
                        Tree querySubTree = matchSeg.getNode(part);
                        ArrayList<Label> wordsInSubTree = querySubTree.yield();
                        String partClauseText = "";
                        for(Label a : wordsInSubTree)
                        {
                            partClauseText += a.value()+" ";
                        }
                        TupleConcept partInClause = new TupleConcept(part, partClauseText.trim());
                        partsClause.add(partInClause);

                    }
                }
                String mergedCond = mergePartsClause(partsClause);
                if(mergedCond.trim().length()>1){
                    MatchPattern matchClauseInReq = new MatchPattern(mergedCond.trim(), typeClause);
                    //adding the concepts
                    matchClauseInReq.setConceptsInText(conceptsReq);
                    if(mergedCond.trim().equals(textPVoiceCond.trim())){
                        matchClauseInReq.setPassiveVoice(passiveVoice);
                    }else{
                        matchClauseInReq.setPassiveVoice("");
                    }
                    matchClauseInReq.setPassiveVoiceSR("");
                    listClausesInReq.add(matchClauseInReq);
                }

            }else if(typeClause == "sysrespSR" || typeClause =="sysrespStartSR" ){
                ArrayList<String> partsSr = new ArrayList<>();
                partsSr.add("nounPhrase");
                partsSr.add("verbPhrase");
                for(String part : partsSr) {
                    if (matchSeg.getNode(part) != null){
                        Tree querySubTree = matchSeg.getNode(part);
                        ArrayList<Label> wordsInSubTree = querySubTree.yield();
                        String partClauseText = "";
                        for(Label a : wordsInSubTree)
                        {
                            partClauseText += a.value()+" ";
                        }
                        TupleConcept partInClause = new TupleConcept(part, partClauseText.trim());
                        partsClause.add(partInClause);

                    }
                }
                String mergedClause = mergePartsClause(partsClause);
                if(mergedClause.length()>1){
                    MatchPattern matchClauseInReq = new MatchPattern(mergedClause.trim(), typeClause);
                    //adding the concepts
                    matchClauseInReq.setConceptsInText(conceptsReq);

                    if(mergedClause.trim().equals(textPVoiceSR.trim())){
                        matchClauseInReq.setPassiveVoiceSR(passiveVoiceSR);
                    }else{
                        matchClauseInReq.setPassiveVoiceSR("");
                    }
                    matchClauseInReq.setPassiveVoice("");
                    listClausesInReq.add(matchClauseInReq);
                }
            }else if(typeClause == "conditionS2" || typeClause=="conditionS4" || typeClause=="conditionS7" ||
                    typeClause =="conditionS8"|| typeClause == "conditionS9"||typeClause=="conditionS10"||
                    typeClause=="conditionS12") //Extracting the clause from the subtree
            {
                ArrayList<Label> yieldElements = matchSeg.getMatch().yield();
                for(Label a : yieldElements)
                {
                    clauseInReq += " "+a.value();
                }


                if(clauseInReq.trim().length()>1){
                    MatchPattern matchClauseInReq = new MatchPattern(clauseInReq.trim(), typeClause);
                    matchClauseInReq.setConceptsInText(conceptsReq);
                    if(clauseInReq.trim().equals(textPVoiceCond.trim())){
                        matchClauseInReq.setPassiveVoice(passiveVoice);
                    }else{
                        matchClauseInReq.setPassiveVoice("");
                    }
                    matchClauseInReq.setPassiveVoiceSR("");
                    listClausesInReq.add(matchClauseInReq);
                }
            }else if(typeClause=="scopeSC1"|| typeClause =="scopeSC2" ){
                ArrayList<Label> yieldElements = matchSeg.getMatch().yield();
                for(Label a : yieldElements)
                {
                    clauseInReq += " "+a.value();
                }
                if(clauseInReq.trim().length()>1){
                    MatchPattern matchClauseInReq = new MatchPattern(clauseInReq.trim(), typeClause);
                    matchClauseInReq.setConceptsInText(conceptsReq);
                    matchClauseInReq.setPassiveVoice("");
                    matchClauseInReq.setPassiveVoiceSR("");
                    listClausesInReq.add(matchClauseInReq);
                }
            }
        }

        ArrayList<MatchPattern> listUniqueClausesInReq = getUniqueClauses(listClausesInReq);
        return listUniqueClausesInReq;
    }


    public static ArrayList<MatchPattern> getUniqueClauses(ArrayList<MatchPattern> clausesInReq)
    {
        ArrayList<MatchPattern> uniqueClauses = new ArrayList<MatchPattern>();
        for (MatchPattern clause :clausesInReq)
        {
            Pair<Integer, Boolean> containsIndex  = Utils.isClauseInList(uniqueClauses, clause.getTextMatch());

            if(!containsIndex.getValue())
            {
                uniqueClauses.add(clause); //The segment/clause is unique

            }else
            { //If there are similar segments/clauses then we need to merge concepts and return only one

                int indexMatch = containsIndex.getKey();
            }
        }
        return  uniqueClauses;
    }



    //Method that merges different parts of segments of requirements
    public static String mergePartsClause(List<TupleConcept> partsClause )
    {
        String mergedClause ="";
        boolean hasClause = false;
        String clause = "";
        boolean hasConjunction = false;
        String conjunction = "";
        boolean hasNounPhraseCond = false;
        String nounPhraseCond ="";
        boolean hasVerbPhraseCond = false;
        String verbPhraseCond ="";


        boolean hasNounPhrase = false;
        String nounPhrase = "";
        boolean hasVerbPhrase = false;
        String verbPhrase = "";
        int count = 0;
        for(TupleConcept partMatched:partsClause)
        {
            count = count +1;
            switch (partMatched.getNameConcept())
            {
                case "conjunction":
                    hasConjunction = true;
                    conjunction = partMatched.getContentConcept();
                    break;
                case "clause":
                    hasClause = true;
                    clause = partMatched.getContentConcept();
                    break;
                case "nounPhraseCond":
                    hasNounPhraseCond = true;
                    nounPhraseCond = partMatched.getContentConcept();
                    break;
                case "verbPhraseCond":
                    hasVerbPhraseCond = true;
                    verbPhraseCond = partMatched.getContentConcept();
                    break;
                case "nounPhrase":
                    hasNounPhrase = true;
                    nounPhrase = partMatched.getContentConcept();
                    break;
                case "verbPhrase":
                    hasVerbPhrase = true;
                    verbPhrase = partMatched.getContentConcept();
                    break;
            }
        }
        if (hasConjunction && hasClause && !hasNounPhrase && !hasVerbPhrase && !hasNounPhraseCond &&  !hasVerbPhraseCond)
        {
            //Condition
            //Condition: conj + clause
            mergedClause += conjunction+" "+clause;
        }else if(hasConjunction && hasNounPhraseCond  && hasVerbPhraseCond  && !hasClause &&!hasNounPhrase &&!hasVerbPhrase)
        {
            //Condition: conjunction+" "+nounPhraseCond+" "+verbPhraseCond
            //Condition not under a Clause
            mergedClause += conjunction+" "+nounPhraseCond+" "+verbPhraseCond;
        }else if(hasConjunction && hasNounPhraseCond && !hasVerbPhraseCond && !hasNounPhrase && !hasVerbPhrase && !hasClause)
        {
            //Incomplete Condition Maybe to remove
            // condition: conjunction+" "+nounPhraseCond;
            mergedClause += conjunction+" "+nounPhraseCond;
        }else if(hasNounPhrase && hasVerbPhrase && !hasConjunction && !hasClause && !hasNounPhraseCond && !hasVerbPhraseCond)
        {
            //System Response
            mergedClause += nounPhrase+" "+verbPhrase;
        }
        return mergedClause;
    }
    //Get requirement with no condition nor scope
    public static String getReqNoConditionOrScope(String origClause, String splitClause)
    {
        String newString = "";
        String[] wordOrigClause  = origClause.split(" ");
        String[] wordSplitClause = splitClause.split(" "); //it should contain less words than the Orig

        if(wordOrigClause.length >= wordSplitClause.length)
        {
            for(int i=0; i < wordSplitClause.length ; i++)
            {
                if(wordOrigClause[i].equals(wordSplitClause[i]))
                {
                    newString+=wordOrigClause[i]+" ";
                }
            }
        }
        return newString.trim();
    }

    //remove the condition from system response
    public static ArrayList<MatchPattern> removeCondAfterSR(ArrayList<MatchPattern> listUniqueClausesInReq)
    {
        //We can have more than one condition
        ArrayList<String> conditionsAfterSR = new ArrayList<String>();
        for(MatchPattern clause:listUniqueClausesInReq)
        {
            if(clause.getTypeOfPattern().equals("conditionS4"))
            {
                conditionsAfterSR.add(clause.getTextMatch());

            }
        }

        if(conditionsAfterSR.size() > 0)
        {
            for( int i = 0; i< listUniqueClausesInReq.size(); i++)
            {
                if((listUniqueClausesInReq.get(i).getTypeOfPattern().equals("sysrespSR")) ||
                        (listUniqueClausesInReq.get(i).getTypeOfPattern().equals("sysrespStartSR")))
                {
                    String updatedSR = listUniqueClausesInReq.get(i).getTextMatch();
                    for(String condAft : conditionsAfterSR)
                    {
                        updatedSR = updatedSR.replace(condAft,"");
                    }

                    String newSR = getReqNoConditionOrScope(listUniqueClausesInReq.get(i).getTextMatch(), updatedSR);
                    listUniqueClausesInReq.get(i).setTextMatch(newSR);
                }
            }
        }
        return listUniqueClausesInReq;
    }

    //remove condition after the scope
    public static ArrayList<MatchPattern> removeCondAfterScope(ArrayList<MatchPattern> listUniqueClausesInReq)
    {
        //We can have more than one condition
        ArrayList<String> conditionsAfterScope = new ArrayList<String>();
        for(MatchPattern clause:listUniqueClausesInReq)
        {
            if(clause.getTypeOfPattern().equals("conditionS8"))
            {
                conditionsAfterScope.add(clause.getTextMatch());

            }
        }

        if(conditionsAfterScope.size() > 0)
        {
            for( int i = 0; i< listUniqueClausesInReq.size(); i++)
            {
                if(listUniqueClausesInReq.get(i).getTypeOfPattern().equals("scopeSC1"))
                {
                    String updatedScope = listUniqueClausesInReq.get(i).getTextMatch();
                    for(String condAft : conditionsAfterScope)
                    {

                        updatedScope = updatedScope.replace(condAft,"");

                    }

                    String newSR = getReqNoConditionOrScope(listUniqueClausesInReq.get(i).getTextMatch(), updatedScope);

                    //String updatedScope = listUniqueClausesInReq.get(i).getTextMatch().replace(conditionsAfterScope,"");
                    listUniqueClausesInReq.get(i).setTextMatch(newSR);
                }
            }
        }
        return listUniqueClausesInReq;
    }

    //Method that removes scope from SR
    public static ArrayList<MatchPattern> removeScopeFromSR(ArrayList<MatchPattern> listUniqueClausesInReq)
    {
        // we can have more than one match for the scope
        ArrayList<String> scopesInSr = new ArrayList<String>();
        for(MatchPattern clause:listUniqueClausesInReq)
        {

            if(clause.getTypeOfPattern().equals("scopeSC2"))
            {

                scopesInSr.add(clause.getTextMatch());
            }
        }

        if(scopesInSr.size() > 0)
        {
            for(int i=0; i<listUniqueClausesInReq.size(); i++)
            {
                if((listUniqueClausesInReq.get(i).getTypeOfPattern().equals("sysrespSR")) ||
                        (listUniqueClausesInReq.get(i).getTypeOfPattern().equals("sysrespStartSR")))
                {
                    String updatedSR = listUniqueClausesInReq.get(i).getTextMatch();
                    for(String scopeInReq:scopesInSr)
                    {
                        updatedSR = updatedSR.replace(scopeInReq,"");
                    }

                    String newSRNoScope = getReqNoConditionOrScope(listUniqueClausesInReq.get(i).getTextMatch(), updatedSR);
                    listUniqueClausesInReq.get(i).setTextMatch(newSRNoScope);
                }
            }
        }
        return listUniqueClausesInReq;
    }



    //Returs true is a segment/clause has a concepts. A concept can be actor, modal verb
    public static Boolean hasClauseConcepts(List<TupleConcept> concepts, String concept )
    {

        boolean hasConcept = false;
        int counterConcept = 0;
        for(TupleConcept c : concepts)
        {

            if(c.getNameConcept().toLowerCase().equals(concept.toLowerCase().trim()))
            {

                counterConcept ++;
            }
        }
        if(counterConcept >= 1)
        {
            hasConcept = true;
        }

        return hasConcept;

    }


}
