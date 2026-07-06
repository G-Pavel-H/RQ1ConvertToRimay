import edu.stanford.nlp.ling.TaggedWord;

import java.util.ArrayList;

public class PosMethodsCondition {
    //Check if a condition is of type S8
    public static boolean isSegmentCondS8(ArrayList<TaggedWord> segmentTagged){
        boolean isCondition=false;
        //Returns the first position of the subordinated conjunction
        int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
        //int firstVerb = PosMethods.getPositionContains(segmentTagged,"/VB");
        int firstVerb = PosMethodsCondition.getVerbInCond(segmentTagged);

        if(nounCond==-1 || ((firstVerb!=-1) && (nounCond > firstVerb))){
            nounCond = PosMethods.getPositionContains(segmentTagged,"there/EX");
        }

        int subConjCond = PosMethods.firstPosSubConj(segmentTagged);
        int verbCond = -1;
        if(nounCond != -1){
            verbCond = PosMethods.getPosWordFromSpecLoc(segmentTagged,"/VB", nounCond);
        }

        if(!PosMethods.hasModalVerb (segmentTagged)){
            if(subConjCond!=-1 && nounCond!=-1 && verbCond!=-1){
                if((subConjCond < nounCond) && (nounCond < verbCond)){
                    //Check the position of the verb and if there are some symbols "=" "<" ">" instead of the verb
                    if(!condVerbWithSymbol(segmentTagged)){
                        isCondition = true;
                    }
                }
            }
        }
        return isCondition;
    }

    //if condition starts with "as soon as"
    public static boolean isSegmentCondAsSoon(ArrayList<TaggedWord> segmentTagged){
        boolean isCondition = false;
        if(segmentTagged.size()>=4){
            if(segmentTagged.get(0).word().toLowerCase().equals("as")&&
                    segmentTagged.get(1).word().toLowerCase().equals("soon") &&
                    segmentTagged.get(2).word().toLowerCase().equals("as")){

                int subConjCond = 0;
                int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
                //int firstVerb = PosMethods.getPositionContains(segmentTagged,"/VB");
                int firstVerb = PosMethodsCondition.getVerbInCond(segmentTagged);

                if(nounCond==-1 || ((firstVerb!=-1) && (nounCond > firstVerb))){
                    nounCond = PosMethods.getPositionContains(segmentTagged,"there/EX");
                }
                int verbCond = -1;
                if(nounCond != -1){
                    verbCond = PosMethods.getPosWordFromSpecLoc(segmentTagged,"/VB", nounCond);

                }
                if(!PosMethods.hasModalVerb (segmentTagged)){
                    if(subConjCond!=-1 && nounCond!=-1 && verbCond!=-1){
                        if((subConjCond < nounCond) && (nounCond < verbCond)){
                            //Check the position of the verb and if there are some symbols "=" "<" ">" instead of the verb
                            if(!condVerbWithSymbol(segmentTagged)){
                                isCondition = true;
                            }
                        }
                    }
                }

            }
        }

        return isCondition;
    }

    //returns a condition that starts with "as soon as"
    public static Condition getSegmentCondAsSoon(ArrayList<TaggedWord> segmentTagged){
        Condition condNew = new Condition(false,false,false);
        condNew.setConditionTagged(segmentTagged);

        if(segmentTagged.size()>=4){
            if(segmentTagged.get(0).word().toLowerCase().equals("as")&&
                    segmentTagged.get(1).word().toLowerCase().equals("soon") &&
                    segmentTagged.get(2).word().toLowerCase().equals("as")){
                int subConjCond = 0;
                int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
                int firstVerb = PosMethodsCondition.getVerbInCond(segmentTagged);
                if(nounCond==-1 || ((firstVerb!=-1) && (nounCond > firstVerb)) ){
                    nounCond = PosMethods.getPositionContains(segmentTagged,"there/EX");
                }

                int verbCond = -1;
                String mainVerb = "";
                if(nounCond != -1){
                    verbCond = PosMethods.getPosWordFromSpecLoc(segmentTagged,"/VB", nounCond);
                    mainVerb = PosMethods.getElementSpecLoc(segmentTagged,"/VB", nounCond);
                }

                if(!PosMethods.hasModalVerb (segmentTagged)){
                    if(subConjCond!=-1 && nounCond!=-1 && verbCond!=-1){
                        if((subConjCond < nounCond) && (nounCond < verbCond)){
                            //Check the position of the verb and if there are some symbols "=" "<" ">" instead of the verb
                            if(!condVerbWithSymbol(segmentTagged)){
                                condNew.setSubConj(true);
                                condNew.setNoun(true);
                                condNew.setVerb(true);

                                if(PosMethods.isPassiveVoice(segmentTagged)){
                                    condNew.setpVoice(true);
                                    String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);

                                    String stemmedVerb = Utils.wordStemmer(mainVb);
                                    condNew.setTextVerb(stemmedVerb);
                                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                                        condNew.setAmbVerb(true);
                                    }
                                }else {
                                    String stemmedVerb = Utils.wordStemmer(mainVerb);
                                    condNew.setTextVerb(stemmedVerb);
                                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                                        condNew.setAmbVerb(true);
                                    }
                                }
                            }
                        }
                    }
                }



            }
        }
        return condNew;
    }

    //Check if a condition has a modal verb
    public static boolean isSegmentCondModal(ArrayList<TaggedWord> segmentTagged){
        //If_IN
        boolean isCondition=false;
        //Returns the first position of the subordinated conjunction
        int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
        int firstVerb = PosMethods.getPositionContains(segmentTagged,"/VB");
        int modalVerb = PosMethods.getPositionContains(segmentTagged, "/MD");

        int subConjCond = PosMethods.firstPosSubConj(segmentTagged);

        int verbCond = -1;
        if(nounCond != -1){
            verbCond = PosMethods.getPosWordFromSpecLoc(segmentTagged,"/VB", nounCond);

        }

        if(PosMethods.hasModalVerb (segmentTagged)){
            if(subConjCond!=-1 && nounCond!=-1 && verbCond!=-1 && modalVerb !=-1){
                if((subConjCond < modalVerb) && (subConjCond < nounCond) && (nounCond < verbCond)){
                    //Check the position of the verb and if there are some symbols "=" "<" ">" instead of the verb
                    if(!condVerbWithSymbol(segmentTagged)){
                        isCondition = true;
                    }
                }
            }
        }

        return isCondition;
    }

    //return a condition of type S8
    public static Condition getSegmentCondS8(ArrayList<TaggedWord> segmentTagged){
        int subConjCond = PosMethods.firstPosSubConj(segmentTagged);
        int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
        int firstVerb = PosMethodsCondition.getVerbInCond(segmentTagged);
        if(nounCond==-1 || ((firstVerb!=-1) && (nounCond > firstVerb)) ){
            nounCond = PosMethods.getPositionContains(segmentTagged,"there/EX");
        }

        int verbCond = -1;
        String mainVerb = "";
        if(nounCond != -1){
            verbCond = PosMethods.getPosWordFromSpecLoc(segmentTagged,"/VB", nounCond);
            mainVerb = PosMethods.getElementSpecLoc(segmentTagged,"/VB", nounCond);
        }

        //int verbCond = PosMethods.getPositionContains(segmentTagged,"/VB");
        Condition condNew = new Condition(false,false,false);
        condNew.setConditionTagged(segmentTagged);
        if(!PosMethods.hasModalVerb (segmentTagged)){
            if(subConjCond!=-1 && nounCond!=-1 && verbCond!=-1){
                if((subConjCond < nounCond) && (nounCond < verbCond)){
                    //Check the position of the verb and if there are some symbols "=" "<" ">" instead of the verb
                    if(!condVerbWithSymbol(segmentTagged)){
                        condNew.setSubConj(true);
                        condNew.setNoun(true);
                        condNew.setVerb(true);

                        if(PosMethods.isPassiveVoice(segmentTagged)){
                            condNew.setpVoice(true);
                            String mainVb = PosMethods.getVerbPassiveVoice(segmentTagged);

                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            condNew.setTextVerb(stemmedVerb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                condNew.setAmbVerb(true);
                            }
                        }else {
                            String stemmedVerb = Utils.wordStemmer(mainVerb);
                            condNew.setTextVerb(stemmedVerb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)) {
                                condNew.setAmbVerb(true);
                            }
                        }
                    }
                }
            }
        }
        return condNew;
    }

    //check if the condition of type S8 has a symbol that denotes an action
    public static boolean isSegmentCondS8VerbSymbol(ArrayList<TaggedWord> segmentTagged){
        //If_IN
        boolean isCondition=false;
        //Returns the first position of the subordinated conjunction
        int subConjCond = PosMethods.firstPosSubConj(segmentTagged);
        int nounCond = PosMethods.getPositionContains(segmentTagged,"/N");
//        int verbCond = PosMethods.getPositionContains(segmentTagged,"/VB");
        int indexCompareChar = PosMethods.firstSymbolCond(segmentTagged);


        int indexVerb = -1;
        String verb = "";
        if(subConjCond!=-1){
            indexVerb = PosMethods.getPosVerbFromSpecLoc(segmentTagged, "VB", subConjCond);
            verb = PosMethods.getVerbSpecLoc(segmentTagged,"VB", subConjCond);
            boolean hasVerb = false;
            if (indexVerb == -1){
                indexVerb = PosMethods.getPosVerbFromSpecLoc(segmentTagged, "VBD", subConjCond);
                if(verb.trim().length() > 0){
                    hasVerb = true;
                }
                if(!hasVerb){
                    indexVerb = PosMethods.getPosVerbFromSpecLoc(segmentTagged, "VBP", subConjCond);
                    verb = PosMethods.getVerbSpecLoc(segmentTagged,"VBP", subConjCond);
                    if(verb.trim().length() > 0){
                        hasVerb = true;
                    }
                }
            }
        }


        if(!PosMethods.hasModalVerb (segmentTagged)){
            //No verb present
            if(subConjCond!=-1 && nounCond!=-1 && indexVerb==-1 && indexCompareChar!=-1){
                if((subConjCond < nounCond) && (nounCond < indexCompareChar)){
                    //Check the position of the verb and if there are some symbols "=" "<" ">" instead of the verb
                    isCondition = true;
                }
            }else if(subConjCond!=-1 && nounCond!=-1 && indexVerb!=-1 && indexCompareChar!=-1){
                if((subConjCond < nounCond) && (nounCond < indexCompareChar) && (indexCompareChar < indexVerb )){
                    isCondition = true;
                }
            }
        }
        return isCondition;
    }

    //Checks if a  segment/clause is of type condition  S10
    public static boolean isSegmentCondS10(ArrayList<TaggedWord> segmentTagged){
        boolean isCond = false;
        int prepKeywordPos = PosMethods.firstPosPrepCond(segmentTagged);
        int nounPos = PosMethods.getPositionContains(segmentTagged, "/N");
        int verbPos = PosMethods.getPositionContains(segmentTagged, "/VB");

        if(prepKeywordPos > 0){
            if(segmentTagged.get(prepKeywordPos-1).word().contains(",") ||
                    segmentTagged.get(prepKeywordPos-1).word().contains(";")||
                    segmentTagged.get(prepKeywordPos-1).word().contains("<lb>")){

                if(verbPos == -1 && !PosMethods.hasModalVerb (segmentTagged) &&
                        PosMethods.hasPrepInCondition(segmentTagged) && !PosMethods.hasSubConj(segmentTagged)){
                    if(prepKeywordPos!=-1 && nounPos!=-1){
                        if(prepKeywordPos < nounPos){
                            isCond = true;
                        }
                    }
                }
            }
        }

        return  isCond;
    }

    //Checks if a  segment/clause is of type condition  starting with "in case that..."
    public static boolean isSegmentCondS8Sub(ArrayList<TaggedWord> segmentTagged){
        boolean isCond = false;
        int firstKeyPos = PosMethods.getPositionContains(segmentTagged,"in/IN");;
        int secondKeyPos = PosMethods.getPositionContains(segmentTagged, "case/NN");
        int thirdKeyPos = PosMethods.getPositionContains(segmentTagged, "that/IN");
        int nounPos = PosMethods.getPositionContains(segmentTagged, "/N");;
        int verbPos = PosMethods.getPositionContains(segmentTagged, "/VB");
        if(!PosMethods.hasModalVerb (segmentTagged)){
            if(firstKeyPos != -1 && secondKeyPos != -1 && thirdKeyPos != -1 && nounPos != -1 && verbPos != -1){
                if((firstKeyPos < secondKeyPos) && (secondKeyPos < thirdKeyPos) && (thirdKeyPos < nounPos) && (nounPos < verbPos)){
                    //Checking that the main verb is correct
                    if(!condVerbWithSymbol(segmentTagged)){
                        isCond = true;
                    }
                }
            }
        }
        return isCond;
    }




    //Method that checks if the condition is incomplete
    public static String isSegmentCondIncomplete(ArrayList<TaggedWord> segmentTagged) {
        String typeSingleCond = "NotCondition";
        int posFirstSubConj = PosMethods.firstPosSubConj(segmentTagged);
        int firstPrepCondS8Sub = PosMethods.posFirstPrepCondS8Sub(segmentTagged);
        int indexNoun = PosMethods.getPositionContains(segmentTagged,"/N");
        int indexVB = PosMethodsCondition.getVerbInCond(segmentTagged);

        if(indexNoun==-1 || ((indexVB != -1) && (indexNoun > indexVB))){
            indexNoun = PosMethods.getPositionContains(segmentTagged,"they/PRP");
        }


        if(posFirstSubConj!=-1 && firstPrepCondS8Sub!=-1 && (posFirstSubConj < firstPrepCondS8Sub)){
            if(posFirstSubConj!=-1 && indexNoun==-1 && indexVB!=-1)//Contains when, NOT noun and verb
            {
                if(posFirstSubConj < indexVB ){
                    typeSingleCond = "condPosIncomplNoNoun";
                }
            }else if(posFirstSubConj!=-1 && indexNoun != -1 && indexVB != -1){

                if((posFirstSubConj < indexVB) && (indexNoun > indexVB)  ){
                  typeSingleCond = "condPosIncomplNoNoun";
                }


            }else if(posFirstSubConj!=-1 && indexNoun!=-1 && indexVB==-1){//Contains when, noun and NOT verb
                if(posFirstSubConj < indexNoun){
                    typeSingleCond = "condPosIncomplNoVerb";
                }
            }else if(posFirstSubConj!=-1 && indexNoun==-1 && indexVB==-1){ //Contains when, NOT noun and NOT verb
                typeSingleCond = "condPosIncomplNoNounNoVerb";
            }else if(posFirstSubConj < indexNoun && indexNoun < indexVB){
                if(condVerbWithSymbol(segmentTagged)){
                    typeSingleCond = "condPosIncomplNoVerb";
                }else{
                    typeSingleCond = "condCompl";
                }
            }
        }else if(posFirstSubConj!=-1 && firstPrepCondS8Sub!=-1 && (firstPrepCondS8Sub < posFirstSubConj)){
            //Scenario 2: When Cond type IF exists and IN CASE exists. if cond appears after  in case
            if(firstPrepCondS8Sub !=-1 && indexNoun==-1 && indexVB!=-1) {//Contains when, NOT noun and verb
                if(firstPrepCondS8Sub < indexVB){
                    typeSingleCond = "condPosIncomplNoNoun";
                }
            }else if(firstPrepCondS8Sub !=-1 && indexNoun!=-1 && indexVB==-1){//Contains when, noun and NOT verb
                typeSingleCond = "condPosIncomplNoNounNoVerb";
            }else if(firstPrepCondS8Sub !=-1 && indexNoun==-1 && indexVB==-1){ ////Contains when, NOT noun and NOT verb
                typeSingleCond = "condPosIncomplNoNounNoVerb";
            }else if(firstPrepCondS8Sub < indexNoun && indexNoun < indexVB){
                if(condVerbWithSymbol(segmentTagged)){
                    typeSingleCond = "condPosIncomplNoVerb";
                }else{
                    typeSingleCond = "condCompl";
                }
            }
        }else if(posFirstSubConj!=-1 && firstPrepCondS8Sub==-1){
            if(posFirstSubConj!=-1 && indexNoun==-1 && indexVB!=-1)
            {
                if(posFirstSubConj < indexVB ){
                    typeSingleCond = "condPosIncomplNoNoun";
                }
            }else if(posFirstSubConj!=-1 && indexNoun!=-1 && indexVB!=-1){
                if((posFirstSubConj < indexVB) && (indexVB < indexNoun) ){

                    typeSingleCond = "condPosIncomplNoNoun";
                }

            }else if(posFirstSubConj!=-1 && indexNoun!=-1 && indexVB==-1){//Contains when, noun and NOT verb
                if(posFirstSubConj < indexNoun){
                    typeSingleCond = "condPosIncomplNoVerb";
                }
            }else if(posFirstSubConj!=-1 && indexNoun==-1 && indexVB==-1){ //Contains when, NOT noun and NOT verb
                typeSingleCond = "condPosIncomplNoNounNoVerb";
            }else if(posFirstSubConj < indexNoun && indexNoun < indexVB){
                if(condVerbWithSymbol(segmentTagged)){
                    typeSingleCond = "condPosIncomplNoVerb";
                }else{
                    typeSingleCond = "condCompl";
                }
            }
        }else if(firstPrepCondS8Sub!=-1 && posFirstSubConj==-1){
            //Scenario 4: When Cond type IF doesnt exists and IN CASE exists
            if(firstPrepCondS8Sub !=-1 && indexNoun==-1 && indexVB!=-1) {//Contains when, NOT noun and verb
                if(firstPrepCondS8Sub < indexVB){
                    typeSingleCond = "condPosIncomplNoNoun";
                }
            }else if(firstPrepCondS8Sub !=-1 && indexNoun!=-1 && indexVB==-1){//Contains when, noun and NOT verb
                typeSingleCond = "condPosIncomplNoNounNoVerb";
            }else if(firstPrepCondS8Sub !=-1 && indexNoun==-1 && indexVB==-1){ ////Contains when, NOT noun and NOT verb
                typeSingleCond = "condPosIncomplNoNounNoVerb";
            }else if(firstPrepCondS8Sub < indexNoun && indexNoun < indexVB){
                if(condVerbWithSymbol(segmentTagged)){
                    typeSingleCond = "condPosIncomplNoVerb";
                }else{
                    typeSingleCond = "condCompl";
                }
            }
        }
        return typeSingleCond;
    }

    //Checks if conditions have other keywords such as "for" and "must"
    public static String isSegCondIncorrect(ArrayList<TaggedWord> segmentTag){
        String typeCond = "Other";

//        int posFor = PosMethods.getPositionContains(segmentTag,"for/IN");
        int posModVerb = PosMethods.getPositionContains(segmentTag, "/MD");
        int firstPrepCondS8Sub = PosMethods.posFirstPrepCondS8Sub(segmentTag); //in case that
        int firstSubConj = PosMethods.firstPosSubConj(segmentTag);

        String typeSingleCond = isSegmentCondIncomplete(segmentTag);


        //Scenario 1: First the condition and the phrase "in case that"
        if(firstSubConj!=-1 && firstPrepCondS8Sub != -1 && (firstSubConj < firstPrepCondS8Sub ) ){

            if(firstSubConj!=-1 && posModVerb!=-1){ //Contains when, and modal verb
                if(firstSubConj < posModVerb){
                    if(typeSingleCond.equals("condCompl")){
                        typeCond = "condIncorrect"; // there are all the components for the condition but we hve other things
                    }else if(typeSingleCond.contains("condPosIncompl")){
                        typeCond = typeSingleCond;
                    }
                }
            }else if(firstSubConj!=-1  && posModVerb==-1){//Contains when and NOT modal verb
                if(typeSingleCond.contains("condPosIncompl")){
                    typeCond = typeSingleCond;
                }
            }
        //Scenario 2: First the phrase "in case that" and then the condition
        }else if (firstSubConj!=-1 && firstPrepCondS8Sub != -1 && (firstPrepCondS8Sub < firstSubConj)){
            if(firstPrepCondS8Sub!=-1  && posModVerb!=-1){ //Contains when,  and modal verb
                if(firstPrepCondS8Sub < posModVerb){
                    if(typeSingleCond.equals("condCompl")){
                        typeCond = "condIncorrect"; // there are all the components for the condition but we hve other things
                    }else if(typeSingleCond.contains("condPosIncompl")){
                        typeCond = typeSingleCond;
                    }
                }
            }else if(firstPrepCondS8Sub!=-1  && posModVerb==-1){//Contains when and NOT modal verb
                if(typeSingleCond.contains("condPosIncompl")){
                    typeCond = typeSingleCond;
                }
            }
        //Scenario 3: Only condition
        }else if(firstSubConj!=-1 && firstPrepCondS8Sub == -1){
            if(firstSubConj!=-1  && posModVerb!=-1){ //Contains when,  and modal verb
                if(firstSubConj < posModVerb){
                    if(typeSingleCond.equals("condCompl")){
                        typeCond = "conditionWithModalVerb"; // there are all the components for the condition but we hve other things
                        if(PosMethods.isPassiveVoice(segmentTag)){
                            typeCond += " PassiveCond";
//

                            String mainVb = PosMethods.getVerbPassiveVoice(segmentTag);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                typeCond += "WithAmbVerb";
                            }
                        }
                    }else if(typeSingleCond.contains("condPosIncompl")){
                        typeCond = typeSingleCond;
                    }
                }
            }else if(firstSubConj!=-1 && posModVerb==-1){//Contains when and NOT modal verb
                if(typeSingleCond.contains("condPosIncompl")){
                    typeCond = typeSingleCond;
                }
            }
        //Scenario 4: Only the phrase "In case that"
        }else if(firstPrepCondS8Sub != -1 && firstSubConj==-1){
            if(firstPrepCondS8Sub!=-1  && posModVerb!=-1){ //Contains when and modal verb
                if(firstPrepCondS8Sub < posModVerb){
                    if(typeSingleCond.equals("condCompl")){
                        typeCond = "conditionWithModalVerb"; // there are all the components for the condition but we hve other things
                        if(PosMethods.isPassiveVoice(segmentTag)){
                            typeCond += " PassiveCond";
                            String mainVb = PosMethods.getVerbPassiveVoice(segmentTag);
                            String stemmedVerb = Utils.wordStemmer(mainVb);
                            if(FindSmell.notPreciseVerb(stemmedVerb)){
                                typeCond += "WithAmbVerb";
                            }
                        }

                    }else if(typeSingleCond.contains("condPosIncompl")){
                        typeCond = typeSingleCond;
                    }
                }
            }else if(firstPrepCondS8Sub!=-1  && posModVerb==-1){//Contains when, and NOT modal verb
                if(typeSingleCond.contains("condPosIncompl")){
                    typeCond = typeSingleCond;
                }
            }
        }

        return typeCond;
    }

    //returns if an list contains an element
    public static Boolean containsElement(ArrayList<TaggedWord> s, String element){
        boolean hasElement = false;
        for(TaggedWord w:s){
            // \*send or \* send or \ *
            if(w.toString().toLowerCase().contains(element.toLowerCase())){
                hasElement = true;
            }
        }

        return hasElement;
    }

    //returns a single condition
    public static String getSingleCondBulletType(Condition c)
    {
        String tempCond ="";
        if(c.getSubConj() && c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
            //Sub Conj Noun and Verb
            tempCond = "completeCond";

        }else if(c.getSubConj() && c.getNoun() && !c.getVerb() && c.isVerbSymbol()){
            //Sub Conj Noun and Verb Symbol
            tempCond = "incomplCondWithSubConjWithSymbol";

        }else if(c.getSubConj() && !c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
            //Sub Conj  and  verb
            tempCond = "incomplCondWithSubConj";

        }else if(c.getSubConj() && c.getNoun() && !c.getVerb() && !c.isVerbSymbol()){
            //Sub Conj Noun and no verb
            tempCond = "incomplCondWithSubConj";

        }else if(c.getSubConj() && !c.getNoun() && !c.getVerb() && !c.isVerbSymbol()){
            //Sub Conj  no noun and no verb

            tempCond = "incomplCondWithSubConj";
        }else if(!c.getSubConj() && c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
            //noun and verb

            tempCond = "completeCondNoSubConj";

        }else if(!c.getSubConj() && c.getNoun() && !c.getVerb() && c.isVerbSymbol()){
            //noun and verb symbol
            tempCond = "incomplCondNoSubConjWithSymbol";

        }else if(!c.getSubConj() && !c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
            tempCond = "incomplCondNoSubConj";

        }else if(!c.getSubConj() && !c.getNoun() && !c.getVerb()){

            tempCond="noCondition";
        }
        return  tempCond;
    }

    //Method that labels the type of multiple conditions
    public static String getTypeMultCondBullet(ArrayList<Condition> conds){
        //TODO add the new smell ambiguos verb
        ArrayList<String> subSeqConds = new ArrayList<>();
        String typeMultCond = "";
        int completeCond = 0;
        int completeCondNoSubConj =0;
        int incomplCondWithSubConj = 0;
        int incomplCondNoSubConj = 0;
        int noConditions =0;
        int noNoun = 0;
        int noVerb = 0;
        int pVoice = 0;
        int ambVerb = 0;
        int verbSymbol = 0;
        String tempCond = "";
        int inxCond = 0;

        //Factorized Conditions
        int countCond = 0;
        boolean largeCond = false;
        if (conds.size() >= 3){
            largeCond = true;
        }

        for(Condition c:conds){
            inxCond++;
            if(c.getSubConj() && c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
                //Sub Conj Noun and Verb
                completeCond++;
                tempCond = "completeCond";
                if(c.getpVoice()){
                    pVoice++;
                }
                if(c.getAmbVerb()){
                    ambVerb++;
                }
            }else if(c.getSubConj() && c.getNoun() && !c.getVerb() && c.isVerbSymbol()){
                //Sub Conj Noun and Verb Symbol
                incomplCondWithSubConj++;
                tempCond = "incomplCondWithSubConj";
                verbSymbol++;
            }else if(c.getSubConj() && !c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
                //Sub Conj  and  verb
                incomplCondWithSubConj++;
                tempCond = "incomplCondWithSubConj";
                if(c.getpVoice()){
                    pVoice++;
                }
                if(c.getAmbVerb()){
                    ambVerb++;
                }

                if(countCond!=0){
                    noNoun++;
                }else{
                    if(!largeCond){
                        noNoun++;
                    }
                }

            }else if(c.getSubConj() && c.getNoun() && !c.getVerb() && !c.isVerbSymbol()){
                //Sub Conj Noun and no verb
                incomplCondWithSubConj++;
                tempCond = "incomplCondWithSubConj";

                if(countCond!=0){
                    noVerb++;
                }else{
                    //first condition
                    if(!largeCond){
                        noVerb++;
                    }
                }

            }else if(c.getSubConj() && !c.getNoun() && !c.getVerb() && !c.isVerbSymbol()){
                //Sub Conj  no noun and no verb
                incomplCondWithSubConj++;
                tempCond = "incomplCondWithSubConj";

                if(countCond!=0){
                    noNoun++;
                    noVerb++;
                }else{
                    if(!largeCond){
                        if(c.getConditionTagged().size() > 2){
                            noNoun++;
                            noVerb++;
                        }
                    }
                }

            }else if(!c.getSubConj() && c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
                //noun and verb
                completeCondNoSubConj++;
                tempCond = "completeCondNoSubConj";
                if(c.getpVoice()){
                    pVoice++;
                }
                if(c.getAmbVerb()){
                    ambVerb++;
                }
            }else if(!c.getSubConj() && c.getNoun() && !c.getVerb() && c.isVerbSymbol()){
                //noun and verb symbol
                incomplCondNoSubConj++;
                tempCond = "incomplCondNoSubConjWithsymbol";
                verbSymbol++;

            }else if(!c.getSubConj() && !c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
                //verb
                incomplCondNoSubConj++;
                tempCond = "incomplCondNoSubConj";
                noNoun++;
                if(c.getpVoice()){
                    pVoice++;
                }
                if(c.getAmbVerb()){
                    ambVerb++;
                }
            }else if(!c.getSubConj() && !c.getNoun() && !c.getVerb()){

                noConditions++;
                tempCond="noCondition";
            }

            countCond ++;
            subSeqConds.add(tempCond+"_"+inxCond);
        }

        if(completeCond > 0 && (completeCondNoSubConj + incomplCondWithSubConj + incomplCondNoSubConj) >= 1 ){
            typeMultCond = "multCondPos";
            if(noVerb>0 || verbSymbol>0){
                typeMultCond += "WithNoVerb";
            }
            if(pVoice>0){
                typeMultCond += "WithPassive";
            }if(ambVerb>0){
                typeMultCond += "WithAmbVerb";
            }


        }else if(incomplCondWithSubConj > 0 && verbSymbol > 0  && (completeCondNoSubConj+incomplCondWithSubConj+incomplCondNoSubConj) >=1 ){
            typeMultCond = "multCondPos";
            if(noVerb>0 || verbSymbol>0){
                typeMultCond += "WithNoVerb";
            }
            if(pVoice>0){
                typeMultCond += "WithPassive";
            }if(ambVerb>0){
                typeMultCond += "WithAmbVerb";
            }
        }else if(incomplCondWithSubConj > 0 && (completeCondNoSubConj+incomplCondWithSubConj+incomplCondNoSubConj) >=2 ){
            typeMultCond = "multCondPos";
            if(noVerb>0 || verbSymbol>0){
                typeMultCond += "WithNoVerb";
            }
            if(pVoice>0){
                typeMultCond += "WithPassive";
            }if(ambVerb>0){
                typeMultCond += "WithAmbVerb";
            }

        }else if(completeCond >0 && (incomplCondWithSubConj > 0 && (completeCondNoSubConj+incomplCondWithSubConj+incomplCondNoSubConj) >=1)){
            typeMultCond = "multCondPos";
            if(noVerb>0 || verbSymbol>0){
                typeMultCond += "WithNoVerb";
            }
            if(pVoice>0){
                typeMultCond += "WithPassive";
            }if(ambVerb>0){
                typeMultCond += "WithAmbVerb";
            }

        }else if(completeCond>=2){
            typeMultCond = "multCondPos";
            if(noVerb>0 || verbSymbol>0){
                typeMultCond += "WithNoVerb";
            }
            if(pVoice>0){
                typeMultCond += "WithPassive";
            }if(ambVerb>0){
                typeMultCond += "WithAmbVerb";
            }
        }


        return typeMultCond;
    }

    //This method checks if there is subsequent condition
    public static String getTypeMultCondContinuation(ArrayList<Condition> condsCont){
        String typeMultCond = "";
        int completeCondNoSubConj =0;
        int incomplCondWithSubConj = 0;
        int incomplCondNoSubConj = 0;

        int noVerb = 0;
        int pVoice = 0;
        int ambVerb = 0;
        int verbSymbol = 0;
        String tempCond = "";

        for(Condition c:condsCont){
            if(!c.getSubConj() && c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
                //noun and verb

                completeCondNoSubConj++;
                tempCond = "completeCondNoSubConj";
                if(c.getpVoice()){
                    pVoice++;
                }
                if(c.getAmbVerb()){
                    ambVerb++;
                }
            }else if(!c.getSubConj() && c.getNoun() && !c.getVerb() && c.isVerbSymbol()){
                //noun and verb symbol

                incomplCondNoSubConj++;
                tempCond = "incomplCondNoSubConjWithsymbol";
                verbSymbol++;

            }else if(!c.getSubConj() && !c.getNoun() && c.getVerb() && !c.isVerbSymbol()){
                //verb

                incomplCondNoSubConj++;
                tempCond = "incomplCondNoSubConj";
                if(c.getpVoice()){
                    pVoice++;
                }
                if(c.getAmbVerb()){
                    ambVerb++;
                }
            }else if(!c.getSubConj() && !c.getNoun() && !c.getVerb()){
                //tempCond="noCondition";
            }
        }
        if((completeCondNoSubConj + incomplCondWithSubConj + incomplCondNoSubConj) >= 1 ){
            typeMultCond = "multCondContPos";
            if(noVerb>0 || verbSymbol>0){
                typeMultCond += "WithNoVerb";
            }
            if(pVoice>0){
                typeMultCond += "WithPassive";
            }if(ambVerb>0){
                typeMultCond += "WithAmbVerb";
            }
        }

        return typeMultCond;
    }




    public static ArrayList<Condition> detectMultCondReq(ArrayList<TaggedWord> tagCond){
        boolean hasMultipleCond = false;
        int countCond = 0;

        ArrayList<Condition> conds = new ArrayList<>();
        if(isSegmentCondS8(tagCond) || isSegmentCondS8VerbSymbol(tagCond)){
            if(containsElement(tagCond, "\\*")){
                //Scenario 1: bullets points
                ArrayList<ArrayList<TaggedWord>> segsInCond =  PosMethods.getTextFromBullets(tagCond);
                for(int i = 0; i < segsInCond.size(); i++){

                    Condition a = getTypeCondBullet(segsInCond.get(i));
                    conds.add(getTypeCondBullet(segsInCond.get(i)));
                }
                if(getTypeMultCondBullet(conds).trim().length() == 0){

                    if(PosMethods.hasSegActionsAndSymbol(tagCond)){
                        conds = getSingleActionsCond(tagCond);
                    }
                }
            }else if(PosMethods.hasSegActionsAndSymbol(tagCond)){

                //Scenario 2: When A send message to B and receives G from B
                conds = getSingleActionsCond(tagCond);
            }
        }
        return conds;
    }

    //get the position of the verb in a condition
    public static int getVerbInCond(ArrayList<TaggedWord> segments, String actor){
        String[] verbs =  new String[]{"VB","VBD","VBP","VBZ"};
        int indexNoun = PosMethods.getPositionContains(segments, actor);
        int index_verb = -1;
        if(indexNoun!=-1){
            if(indexNoun+1 < segments.size()){
                outerloop:
                for(int i=(indexNoun+1); i<segments.size(); i++){
                    for(String v: verbs){
                        if(segments.get(i).tag().toLowerCase().equals(v.toLowerCase())){
                            index_verb = i;
                            break outerloop;
                        }
                    }
                }
            }
        }

        return index_verb;
    }

    //get the position of the verb in the condition
    public static int getVerbInCond(ArrayList<TaggedWord> segments){
        String[] verbs =  new String[]{"VB","VBD","VBP","VBZ"};

        int index_verb = -1;

        if( segments.size() > 1){
            outerloop:
            for(int i=0; i<segments.size(); i++){
                for(String v: verbs){
                    if(segments.get(i).tag().toLowerCase().equals(v.toLowerCase())){
                        index_verb = i;
                        break outerloop;
                    }
                }
            }
        }


        return index_verb;
    }


    //Get the type of condition that it is within a bullet point
    public static Condition getTypeCondBullet(ArrayList<TaggedWord> segments){
        String bulletType = "NotMatch";


        int indexSubConj = PosMethods.firstPosSubConj(segments);
        int indexNoun = PosMethods.getPositionContains(segments, "/N");
        int indexVerb = -1;


        if(indexNoun != -1){
            indexVerb = getVerbInCond(segments, "/N");
            if(indexVerb!= -1){
                String v = segments.get(indexVerb).word().toLowerCase();
                if(v.equals("do") || v.equals("does")){
                    if(indexVerb+2 < segments.size()){
                        if (segments.get(indexVerb+1).word().toLowerCase().equals("not")){
                            if(segments.get(indexVerb+2).tag().toLowerCase().contains("vb")){
                                indexVerb = indexVerb + 2;
                            }
                        }
                    }
                }
            }
        }else{
            indexVerb = getVerbInCond(segments);
            if(indexVerb!= -1){
                String v = segments.get(indexVerb).word().toLowerCase();
                if(v.equals("do") || v.equals("does")){
                    if(indexVerb+2 < segments.size()){
                        if (segments.get(indexVerb+1).word().toLowerCase().equals("not")){
                            if(segments.get(indexVerb+2).tag().toLowerCase().contains("vb")){
                                indexVerb = indexVerb + 2;
                            }
                        }
                    }

                }
            }
        }

        //Scenario where there is a noun after verb and not actor
        if(indexVerb == -1){
            indexVerb = getVerbInCond(segments);
            if(indexVerb!= -1){
                String v = segments.get(indexVerb).word().toLowerCase();
                if(v.equals("do") || v.equals("does")){
                    if(indexVerb+2 < segments.size()){
                        if (segments.get(indexVerb+1).word().toLowerCase().equals("not")){
                            if(segments.get(indexVerb+2).tag().toLowerCase().contains("vb")){
                                indexVerb = indexVerb + 2;
                            }
                        }
                    }
                }
            }

        }

        if(indexSubConj != -1){
            if( ((indexVerb!=-1) && (indexSubConj > indexVerb))||
                    ( (indexNoun!=-1)&& (indexSubConj > indexNoun))){
                indexSubConj = -1;
            }
        }

        if (indexNoun>indexVerb){
            int indexTemp = PosMethods.getPositionContains(segments,"there/EX");
            if(indexTemp!=-1){
                indexNoun = indexTemp;
            }
        }

        int indexCompareChar = PosMethods.firstSymbolCond(segments);

        if(indexVerb > 2 && ((indexVerb+ 1) < segments.size())){
            if(segments.get(indexVerb - 1).word().equals(",") && segments.get(indexVerb - 2).tag().toLowerCase().contains("nn")
                    && segments.get(indexVerb + 1).tag().toLowerCase().contains(",")){
                indexVerb = -1;

            }
        }


        if(indexVerb!=-1){
            if(segments.get(indexVerb).word().length()>0){
                String v= segments.get(indexVerb).word();
                if(Character.isUpperCase(v.charAt(0))){
                    indexVerb = -1;
                }

            }
        }

        String verb = "";
        if(indexVerb!=-1){
            verb =  segments.get(indexVerb).word();
        }

        Condition c = new Condition(false,false,false);

        if(indexSubConj!=-1 && indexNoun == -1 && indexVerb == -1){
        //Scenario 1a: SubConj No noun and no Verb
            c.setSubConj(true);
            c.setNoun(false);
            c.setVerb(false);
            c.setConditionTagged(segments);
            bulletType = "condBulletNoNounNoVerb";
        }else if(indexSubConj!=-1  && indexNoun != -1 && indexVerb == -1 && indexCompareChar ==-1){
        //Scenario 1b: SubConj Noun and No Verb
            if(indexSubConj < indexNoun){
                c.setSubConj(true);
                c.setNoun(true);
                c.setVerb(false);
                c.setConditionTagged(segments);
                bulletType = "condBulletNoVerb";
            }
        }else if(indexSubConj!=-1  && indexNoun != -1 && indexVerb == -1 && indexCompareChar !=-1){
            if(indexSubConj < indexNoun && indexNoun < indexCompareChar){
                c.setSubConj(true);
                c.setNoun(true);
                c.setVerb(false);
                c.setVerbSymbol(true);
                c.setConditionTagged(segments);
            }

        }else if(indexSubConj!=-1  && indexNoun == -1 && indexVerb != -1) {
        //Scenario 1c: SubConj No Noun Verb
            c.setSubConj(true);
            c.setNoun(false);
            c.setConditionTagged(segments);
            if(indexSubConj < indexVerb ){
                if(PosMethods.isPassiveVoice(segments)){
                    String mainVb = PosMethods.getVerbPassiveVoice(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        c.setVerb(true);
                        c.setpVoice(true);
                        c.setAmbVerb(true);
                        bulletType = "condBulletNoNounPassiveAmbVerb";
                    }else {
                        c.setVerb(true);
                        c.setpVoice(true);
                        bulletType = "condBulletNoNounPassive";
                    }
                }else{
                    String stemmedVerb = Utils.wordStemmer(verb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        c.setVerb(true);
                        c.setAmbVerb(true);
                        bulletType = "condBulletNoNounAmbVerb";
                    }else{
                        c.setVerb(true);
                        bulletType = "condBulletNoNoun";
                    }
                }
            }
        }
        else if(indexSubConj!=-1 && indexNoun!=-1 && indexVerb != -1){
        //Scenario 2: SubConj Noun and Verb
            c.setSubConj(true);
            c.setNoun(true);
            c.setConditionTagged(segments);
            if(indexSubConj < indexNoun && indexNoun < indexVerb){
                if(condVerbWithSymbol(segments)){
                    c.setVerb(false);
                    c.setVerbSymbol(true);
                    bulletType = "condBulletNoVerb";
                }else if(PosMethods.isPassiveVoice(segments)) {
                    c.setVerb(true);
                    String mainVb = PosMethods.getVerbPassiveVoice(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        c.setpVoice(true);
                        c.setAmbVerb(true);
                        bulletType = "condBulletPassiveVoiceAmbVerb";
                    }else {
                        c.setpVoice(true);
                        bulletType = "condBulletPassiveVoice";
                    }
                }else{
                    String stemmedVerb = Utils.wordStemmer(verb);
                    c.setVerb(true);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        c.setAmbVerb(true);
                        bulletType = "condBulletAmbVerb";
                    }else{
                        bulletType = "condBullet";
                    }
                }
            }//(DT the) (NN balance)) (PP (IN of) (NP (DT the) (VBN reported) (NNP day=0)
        }else if(indexSubConj ==-1 && indexNoun!=-1 && indexVerb != -1 && (indexNoun < indexVerb)){
            //Scenario 3: Noun and verb
            c.setSubConj(false);
            c.setNoun(true);
            c.setConditionTagged(segments);
            if(indexNoun < indexVerb){
                if(condVerbWithSymbolBullet(segments)){
                    c.setVerb(false);
                    c.setVerbSymbol(true);
                    bulletType = "condBulletNoSubConjNoVerb";
                }else if(PosMethods.isPassiveVoice(segments)){
                    c.setVerb(true);
                    c.setpVoice(true);
                    String mainVb = PosMethods.getVerbPassiveVoice(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        c.setAmbVerb(true);
                        bulletType = "condBulletNoSubConjPassiveVoiceAmbVerb";
                    }else {
                        bulletType = "condBulletNoSubConjPassiveVoice";
                    }
                }else{
                    c.setVerb(true);
                    String stemmedVerb = Utils.wordStemmer(verb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        c.setAmbVerb(true);
                        bulletType = "condBulletNoSubConjAmbVerb";
                    }else {

                        bulletType = "condBulletNoSubConj";
                    }
                }
            }
        }else if(indexSubConj ==-1 && indexNoun!=-1 && indexVerb != -1 &&  (indexVerb < indexNoun)){
            //Scenario 4: Verb and noun
            if(indexVerb < indexNoun){
                c.setSubConj(false);
                c.setNoun(false);
                c.setConditionTagged(segments);
                if(condVerbWithSymbolBullet(segments)){
                    c.setVerb(false);
                    c.setVerbSymbol(true);
                    bulletType = "condBulletNoSubConjNoVerb";
                }else if(PosMethods.isPassiveVoice(segments)){
                    c.setVerb(true);
                    c.setpVoice(true);
                    String mainVb = PosMethods.getVerbPassiveVoice(segments);
                    String stemmedVerb = Utils.wordStemmer(mainVb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)){
                        c.setAmbVerb(true);
                        bulletType = "condBulletNoSubConjPassiveVoiceAmbVerb";
                    }else {
                        bulletType = "condBulletNoSubConjPassiveVoice";
                    }
                }else{
                    c.setVerb(true);
                    String stemmedVerb = Utils.wordStemmer(verb);
                    if(FindSmell.notPreciseVerb(stemmedVerb)) {
                        c.setAmbVerb(true);
                        bulletType = "condBulletNoSubConjNoNounAmbVerb";
                    }else {
                        bulletType = "condBulletNoSubConjNoNoun";
                    }
                }
            }
        }else if (indexSubConj==-1  && indexNoun != -1 && indexVerb == -1 && indexCompareChar !=-1){

            if( indexNoun < indexCompareChar){
                c.setSubConj(false);
                c.setNoun(true);
                c.setVerb(false);
                c.setVerbSymbol(true);
                c.setConditionTagged(segments);
            }

        }else {
            c.setNotMatched(true);
            c.setConditionTagged(segments);

        }
        return  c;
    }

    //Returns a single action from a condition
    public  static ArrayList<Condition> getSingleActionsCond(ArrayList<TaggedWord> seg){
        ArrayList<Condition> segsIncond = new ArrayList<>();
//        ArrayList<Integer> positionSeps = PosMethods.getIndexAndOrsSeg(seg);
        ArrayList<Integer> positionSeps = PosMethods.getIndexSepCond(seg);


        for (int i=0; i< positionSeps.size(); i++ ){
            if( (i+1) < positionSeps.size()){
                if(i != (positionSeps.size()-2)){

                    int distance =  positionSeps.get(i) - positionSeps.get(i+1);
                    if(distance <= -3){
                        Condition next = getTypeCondBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), (positionSeps.get(i+1) -1)));
                        segsIncond.add(next);
                    }else{
                        Condition next = getTypeCondBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), (positionSeps.get(i+1) -1)));
                        segsIncond.add(next);
                    }
                }else{
                    int distance =  positionSeps.get(i) - positionSeps.get(i+1);
                    if(distance <= -3){
                        Condition next = getTypeCondBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), positionSeps.get(i+1)));
                        segsIncond.add(next);
                    }else{
                        Condition next = getTypeCondBullet(PosMethods.getSegmentFromList(seg, (positionSeps.get(i)), positionSeps.get(i+1)));
                        segsIncond.add(next);
                    }
                }
            }
        }


        ArrayList<Condition> mergedCond = mergeConditionSegments(segsIncond);


        return mergedCond;
    }

    //Merge two given Segments Seg A + Seg B
    public static ArrayList<TaggedWord> mergeConditions(ArrayList<TaggedWord> segA, ArrayList<TaggedWord> segB){
        ArrayList<TaggedWord> merged = new ArrayList<>();

        for(TaggedWord w: segA){
            merged.add(w);
        }
        for(TaggedWord w:segB){
            merged.add(w);
        }
        return merged;
    }

    //Checks if there are words after the verb or symbol (=,<,) in the condition
    public static boolean hasWordAfterVerb(ArrayList<TaggedWord> seg){
        boolean hasNoun = false;
//        int indexVB = PosMethods.getPositionContains(seg, "/VB");
        int indexVB = PosMethodsCondition.getVerbInCond(seg);
        int indexCompareChar = PosMethods.firstSymbolCond(seg);

        int wordsAftVerb = PosMethods.getNumberWordAfterVerb(seg);
        int wordsAfterSymbol = PosMethods.getNumberWordAftSymbol(seg);

        if((indexVB!=-1 && wordsAftVerb>0) || (indexCompareChar!=-1 && wordsAfterSymbol > 0)){
            hasNoun = true;
        }
        return hasNoun;
    }

    //get the last word of the condition
    public static ArrayList<TaggedWord> getLastElement(ArrayList<Condition> listCond){
        ArrayList<TaggedWord> lastElement = new ArrayList<>();
        if(listCond.size()>0){
            lastElement = listCond.get(listCond.size()-1).getConditionTagged();
        }
        return lastElement;

    }

    //Method that merges conditions
    public static ArrayList<Condition> mergeConditionSegments(ArrayList<Condition> segsInCond){
        ArrayList<Condition> mergedConds = new ArrayList<>();
        for(int i=0; i< segsInCond.size();i++){
            if(i!=0){
                //If the segment is not matched or it contains only two words
                if(segsInCond.get(i).getNotMatched() || segsInCond.get(i).getConditionTagged().size() < 3 ){

                    ArrayList<TaggedWord> mergedSeg = mergeConditions(segsInCond.get(i-1).getConditionTagged(),segsInCond.get(i).getConditionTagged());
                    Condition mergedCond = getTypeCondBullet(mergedSeg);

                    if(PosMethods.getTextFromSentence(getLastElement(mergedConds)).equals(PosMethods.getTextFromSentence(segsInCond.get(i-1).getConditionTagged()))){

                        if(mergedConds.size() > 0){
                            mergedConds.remove(mergedConds.size()-1);
                        }
                    }
                    mergedConds.add(mergedCond);
                 // no word after the verb
                }else if(!hasWordAfterVerb(segsInCond.get(i).getConditionTagged())){

                    ArrayList<TaggedWord> mergedSeg = mergeConditions(segsInCond.get(i-1).getConditionTagged(),segsInCond.get(i).getConditionTagged());
                    Condition mergedCond = getTypeCondBullet(mergedSeg);


                    if(PosMethods.getTextFromSentence(getLastElement(mergedConds)).equals(PosMethods.getTextFromSentence(segsInCond.get(i-1).getConditionTagged()))){
                        //then we need to remove last element from the
                        if(mergedConds.size() > 0){
                            mergedConds.remove(mergedConds.size()-1);
                        }
                    }
                    mergedConds.add(mergedCond);
                }else { //We do not merge anything

                    if(i==1){
                        // the first condition does not have a verb and second does have
                        if (conditionNoVerb(segsInCond.get(i-1).getConditionTagged()) &&
                                !segsInCond.get(i-1).isVerbSymbol()){

                            ArrayList<TaggedWord> mergedSeg = mergeConditions(segsInCond.get(i-1).getConditionTagged(),segsInCond.get(i).getConditionTagged());
                            if(PosMethods.getTextFromSentence(getLastElement(mergedConds)).equals(PosMethods.getTextFromSentence(segsInCond.get(i-1).getConditionTagged()))){
                                //then we need to remove last element from the
                                if(mergedConds.size() > 0){
                                    mergedConds.remove(mergedConds.size()-1);
                                }
                            }
                            Condition mergedCond = getTypeCondBullet(mergedSeg);
                            mergedConds.add(mergedCond);

                        }else{
                            mergedConds.add(segsInCond.get(i-1)); //since we do not consider the first one
                            mergedConds.add(segsInCond.get(i));
                        }
                    }else{

                        mergedConds.add(segsInCond.get(i));

                    }
                }

            }
        }
        return mergedConds;
    }

    //Checks if a condition does not have a verb
    public static Boolean conditionNoVerb(ArrayList<TaggedWord> condition){
        boolean condNoVerb = false;
        int indexVerb  = PosMethods.getPositionContains(condition,"VB");
        Boolean hasSub = PosMethods.hasSubConj(condition);

        if(hasSub && indexVerb ==-1){
            condNoVerb = true;
        }
        return condNoVerb;
    }


    // Checks if bullets conditions do not have Subordinated conjunction
    public static boolean condVerbWithSymbolBullet(ArrayList<TaggedWord> cond){
        boolean noMainVerb = false;
        int indexCompareChar = PosMethods.firstSymbolCond(cond);


        if(PosMethods.countVerbsInCond(cond) == 1){
            int verbPast1 = PosMethods.getPositionContains(cond,"/VBN"); //present participle
            int verbPast2 = PosMethods.getPositionContains(cond,"/VBG"); //present participle
            int verbPast3 = PosMethods.getPositionContains(cond,"/VBD"); //past tense
            int indexNoun = PosMethods.getPositionContains(cond, "/N");
            if(indexNoun != -1 && verbPast1 != -1){
                //Only one verb and then it is either in on the last 3 positions of the sentence
                if((verbPast1 == cond.size() -1 || verbPast1 == cond.size() -2 || verbPast1 == cond.size() -3 )
                        && indexCompareChar!=-1){
                    noMainVerb = true;
                }
            }
            if(indexNoun != -1 && verbPast2 != -1){
                //Only one verb and then it is either in on the last 3 positions of the sentence
                if((verbPast2 == cond.size() -1 || verbPast2 == cond.size() -2 || verbPast2 == cond.size() -3 )
                        && indexCompareChar!=-1){
                    noMainVerb = true;
                }
            }
            if(indexNoun != -1 && verbPast3 != -1){
                //Only one verb and then it is either in on the last 3 positions of the sentence
                if((verbPast3 == cond.size() -1 || verbPast3 == cond.size() -2 ||verbPast3 == cond.size() -3 )
                        && indexCompareChar!=-1){
                    noMainVerb = true;
                }
            }
        }
        return noMainVerb;
    }

    //checks if there is a symbol that denotes action in a condition
    public static boolean condVerbWithSymbol(ArrayList<TaggedWord> cond){
        boolean verbSymbol = false;
        int indexSubCond = PosMethods.firstPosSubConj(cond);
        int indexPrepCond = PosMethods.firstPosPrepCondIncomp(cond);
        int indexCompareChar = PosMethods.firstSymbolCond(cond);

        if(PosMethods.countVerbsInCond(cond) == 1  ){
            int verbPast1 = PosMethods.getPositionExact(cond,"VBN"); //present participle
            int verbPast2 = PosMethods.getPositionExact(cond,"VBG"); //present participle
            int verbPast3 = PosMethods.getPositionExact(cond,"VBD"); //past tense
            int indexNoun = PosMethods.getPositionContains(cond, "N");

            if(indexSubCond != -1 && indexNoun != -1 && verbPast1 != -1){
                if((verbPast1 == cond.size() -1 || verbPast1 == cond.size() -2 || verbPast1 == cond.size() -3 )
                        && indexCompareChar!=-1){
                    verbSymbol = true;
                }
            }
            if(indexSubCond != -1 && indexNoun != -1 && verbPast2 != -1){
                if((verbPast2 == cond.size() -1 || verbPast2 == cond.size() -2 || verbPast2 == cond.size() -3 )
                        && indexCompareChar!=-1){
                    verbSymbol = true;
                }
            }
            if(indexSubCond != -1 && indexNoun != -1 && verbPast3 != -1){
                if((verbPast3 == cond.size() -1 || verbPast3 == cond.size() -2 ||verbPast3 == cond.size() -3 )
                        && indexCompareChar!=-1){
                    verbSymbol = true;
                }
            }
            if(indexPrepCond != -1 && indexNoun != -1 && verbPast1 != -1){
                if((verbPast1 == cond.size() -1 || verbPast1 == cond.size() -2 || verbPast1 == cond.size() -3)
                        && indexCompareChar!=-1){
                    verbSymbol = true;
                }
            }
            if(indexPrepCond != -1 && indexNoun != -1 && verbPast2 != -1){
                if((verbPast2 == cond.size() -1 || verbPast2 == cond.size() -2 || verbPast2 == cond.size() -3)
                        && indexCompareChar!=-1){
                    verbSymbol = true;
                }
            }
            if(indexPrepCond != -1 && indexNoun != -1 && verbPast3 != -1){
                if((verbPast3 == cond.size() -1 || verbPast3 == cond.size() -2 ||verbPast3 == cond.size() -3 )
                        && indexCompareChar!=-1){
                    verbSymbol = true;
                }
            }
        }
        return verbSymbol;
    }

    //Checks that a condition has no verb
    public static boolean noVerbCond(ArrayList<TaggedWord> segments){
        boolean incompCond = false;
        int indexSubCond = PosMethods.firstPosSubConj(segments);
        int indexPrepCond = PosMethods.firstPosPrepCondIncomp(segments);
        int indexNoun = PosMethods.getPositionContains(segments, "/N");
        int indexVerb = PosMethods.getPositionContains(segments,"/VB");
        int indexModVerb = PosMethods.firstModalVerb(segments);
        //Scenario 1: WHEN Noun and no verb, no modal verb
        //Scenario 2: Preposition Noun and no verb, no modal verb
        if(indexSubCond != -1 && indexNoun != -1 && indexVerb == -1 && indexModVerb == -1){
            if(indexSubCond < indexNoun){
                incompCond = true;
            }
        }else if(indexPrepCond != -1 && indexNoun != -1 && indexVerb == -1 && indexModVerb == -1){
            if(indexPrepCond < indexNoun){
                incompCond = true;
            }
        }
        return incompCond;
    }

}
