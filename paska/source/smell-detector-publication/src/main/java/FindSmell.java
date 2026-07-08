import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/*
    * Different methods that allow the identificaton of smells
*/
public class FindSmell {

    /* Counts the number of condition before system response
    * CondPosIncomplete, multSrs, sysrespStartSR, multSrsPos, sysrespincompl, multSrsPos, SysResp, multcondpos
    * */
    public static int  numCondBefore(List<SegmentReq> segmentsReq)
    {
        int countCondBefore = 0;
        ArrayList<String> typesSegs = new ArrayList<>();
        for(SegmentReq s: segmentsReq)
        {
            String tSeg = s.getTypeOfClause();
            if(tSeg.toLowerCase().trim().equals("conditions3 conditions8")){
                tSeg = "conditions3";
            }
            String[] typesSeg = (tSeg.trim()).split(" ");
            if(typesSeg.length > 1){
                for(String t:typesSeg){
                    typesSegs.add(t.trim());
                }
            }else{
                typesSegs.add(tSeg);
            }
        }
        int posSresp = getPosSegment(typesSegs,"sysresp");
        if(posSresp!=-1){
            for(int j=posSresp; j>=0 ; j--){
                if(typesSegs.get(j).trim().toLowerCase().contains("multcond")){
                    countCondBefore++;
                    countCondBefore++;

                }else if(typesSegs.get(j).trim().toLowerCase().contains("cond") &&
                        !typesSegs.get(j).trim().toLowerCase().contains("mult")){
                    countCondBefore++;
                }
            }
        }
        int posMultSresp = getPosSegment(typesSegs,"srs");
        if(posMultSresp!=-1){
            for(int j=posMultSresp; j>=0 ; j--){
                if(typesSegs.get(j).trim().toLowerCase().contains("multcond") &&
                        !typesSegs.get(j).trim().toLowerCase().contains("mult")){
                    countCondBefore++;
                    countCondBefore++;

                }else if(typesSegs.get(j).trim().toLowerCase().contains("cond")){
                    countCondBefore++;
                }
            }
        }
        return countCondBefore;
    }

    public static int getPosSegment(ArrayList<String> segsReq, String element){
        int posSeg=-1;
        for(int i=0; i < segsReq.size();i++){
            if((segsReq.get(i).trim().toLowerCase()).contains(element)){
                posSeg = i;
                break;
            }
        }
        return posSeg;
    }

    /*
    * Method that counts the number of Scopes before system response
    * ScopePosIncomplete
    * */

    public static int numScopeBef(List<SegmentReq> segmentsReq, String reqID){
        int countScopeBef= 0;
        ArrayList<String> typesSegs = new ArrayList<>();
        for(SegmentReq s: segmentsReq)
        {
            String tSeg = s.getTypeOfClause();
            String[] typesSeg = (tSeg.trim()).split(" ");
            if(typesSeg.length > 1){
                for(String t:typesSeg){
                    typesSegs.add(t.trim());
                }
            }else{
                typesSegs.add(tSeg);
            }
        }
        int posSresp = getPosSegment(typesSegs,"sysresp");
        if(posSresp!=-1){
            for(int j=posSresp; j>=0 ; j--){

                if(typesSegs.get(j).trim().toLowerCase().contains("scope")){
                    countScopeBef++;
                }
            }
        }
        int multSresp = getPosSegment(typesSegs,"srs");
        if(multSresp!=-1){
            for(int j=multSresp; j>=0 ; j--){
                if(typesSegs.get(j).trim().toLowerCase().contains("scope")){
                    countScopeBef++;
                }
            }
        }
        return countScopeBef;
    }


    /*
     * Method that counts the number of errors:  ScopePosIncomplete
     * */
    public static int numErrors(List<SegmentReq> segmentsReq){
        int countErrors = 0;
        for(SegmentReq s: segmentsReq)
        {
            String tSeg = s.getTypeOfClause();
            String[] typesSeg = (tSeg.trim()).split(" ");
            if(typesSeg.length > 1){
                for(String t:typesSeg){
                    if((t.trim().toLowerCase()).contains("multsegpos") || (t.trim().toLowerCase()).contains("notmatch")){
                        countErrors++;
                    }
                }
            }else{
                if((tSeg.trim().toLowerCase()).contains("multsegpos") || (tSeg.trim().toLowerCase()).contains("notmatch")){
                    countErrors++;
                }
            }
        }
        return countErrors;

    }

    /*
     * Method that counts the number of scopes after the system response
     * ScopePosIncomplete
     * */
    public static int numScopeAft(List<SegmentReq> segmentsReq){
        int countScopeAft = 0;
        ArrayList<String> typesSegs = new ArrayList<>();
        for(SegmentReq s: segmentsReq)
        {
            String tSeg = s.getTypeOfClause();
            String[] types = (tSeg.trim()).split(" ");
            if(types.length > 1){
                for(String t:types){
                    typesSegs.add(t.trim());
                }
            }else{
                typesSegs.add(tSeg);
            }
        }
        //ScopePosIncomplete
        int posSresp = getPosSegment(typesSegs,"sysresp");
        if(posSresp!=-1){

            for(int j=posSresp; j<typesSegs.size();j++){
                if(typesSegs.get(j).trim().toLowerCase().contains("scope")){
                    countScopeAft++;
                }
            }
        }
        int multPosSresp = getPosSegment(typesSegs,"srs");
        if(multPosSresp!=-1){

            for(int j=multPosSresp; j<typesSegs.size();j++){
                if(typesSegs.get(j).trim().toLowerCase().contains("scope")){
                    countScopeAft++;
                }
            }
        }
        return countScopeAft;
    }

    /*
     * Method that counts the number of conditions on requirement that does not have a system response
     * */
    public static int numCondNoSR(List<SegmentReq> segmentsReq){
        boolean hasSr = false;
        int countCond = 0;
        ArrayList<String> typesSegs = new ArrayList<>();
        for(SegmentReq s: segmentsReq)
        {
            String tSeg = s.getTypeOfClause();
            String[] typesSeg = (tSeg.trim()).split(" ");
            if(typesSeg.length > 1){
                for(String t:typesSeg){
                    typesSegs.add(t.trim());
                    if(t.trim().toLowerCase().contains("sysresp")){
                        hasSr = true;
                    }
                }
            }else{
                typesSegs.add(tSeg);
                if(tSeg.toLowerCase().contains("sysresp")){
                    hasSr = true;
                }
            }
        }
        if(!hasSr){
            for(int j=0; j<typesSegs.size();j++){
                if(typesSegs.get(j).contains("cond")){
                    countCond++;
                }

            }

        }
        return  countCond;
    }

    /*
     * Method that counts the number of conditions after the system response
     * CondPosIncomplete, CondPosIncomplete, multSrs, sysrespStartSR, multSrsPos, sysrespincompl
     * multSrsPos, SysResp
     * */

    public static int  numCondAfter(List<SegmentReq> segmentsReq)
    {
        int countCondAfter = 0;
        ArrayList<String> typesSegs = new ArrayList<>();
        for(SegmentReq s: segmentsReq)
        {
            String tSeg = s.getTypeOfClause();
            if(tSeg.toLowerCase().trim().equals("conditions3 conditions8")){
                tSeg = "conditions3";
            }
            String[] typesSeg = (tSeg.trim()).split(" ");
            if(typesSeg.length > 1){
                for(String t:typesSeg){
                    typesSegs.add(t.trim());
                }
            }else{
                typesSegs.add(tSeg);
            }
        }

        int posSresp = getPosSegment(typesSegs,"sysresp");

        if(posSresp!=-1){
            for(int j=posSresp; j<typesSegs.size();j++){

                if(typesSegs.get(j).trim().toLowerCase().contains("multcond")){
                    countCondAfter++;
                    countCondAfter++;
                }else if(typesSegs.get(j).trim().toLowerCase().contains("cond") &&
                        ! typesSegs.get(j).trim().toLowerCase().contains("mult")){
                    countCondAfter++;
                }
            }
        }
        // multsrspos condposincomplnonounnoverb
        int posMultSresp = getPosSegment(typesSegs,"srs");
        if(posMultSresp!=-1){
            for(int j=posMultSresp; j<typesSegs.size();j++){
                if(typesSegs.get(j).trim().toLowerCase().contains("multcond")){
                    countCondAfter++;
                    countCondAfter++;
                }else  if(typesSegs.get(j).trim().toLowerCase().contains("cond") &&
                        !typesSegs.get(j).trim().toLowerCase().contains("mult")){
                    countCondAfter++;
                }
            }
        }

        return countCondAfter;
    }


    //Method that calculates the number of system responses
    public static int numSR(List<SegmentReq> segmentsReq)
    {
        //sysrespStartSR
        ////Scenario  SysRespPos SysRespPos  OR  SysRespPos ConditionPosIncomplete SysRespPos
        int countSR = 0;
        for(SegmentReq segReq: segmentsReq)
        {
            String typeSegReq = segReq.getTypeOfClause();
            String[] typesSReq = (typeSegReq.trim()).split(" ");
            if(typeSegReq.length() > 1){
                for(String t: typesSReq){
                    if(t.toLowerCase().trim().contains("sysresp") || t.toLowerCase().trim().contains("srs")){
                        //sysresp
                        countSR++;
                    }
                }
            }else{
                //multsrspos
                //sysrespincomplnonoun multcondposwithnoverb || typeSegReq.toLowerCase().contains("sysresp")
                if(typeSegReq.toLowerCase().contains("sysrespsr") || typeSegReq.toLowerCase().contains("sysrespstartsr") ||
                        typeSegReq.toLowerCase().contains("sysresppos") ||  typeSegReq.toLowerCase().contains("srs"))
                {
                    countSR++;
                }
            }
        }
        return countSR;
    }

    // Method that counts the number of SR that are at the beggining of the requirement
    public static int numSRStart(List<SegmentReq> segmentsReq)
    {
        //sysrespStartSR
        ////Scenario  SysRespPos SysRespPos  OR  SysRespPos ConditionPosIncomplete SysRespPos
        int countSRStart = 0;
        for(SegmentReq segReq: segmentsReq)
        {
            String typeSegReq = segReq.getTypeOfClause();
            String[] typesSReq = (typeSegReq.trim()).split(" ");
            if(typeSegReq.length() > 1){
                for(String t: typesSReq){
                    if(t.toLowerCase().trim().contains("sysrespstartsr") && !t.toLowerCase().trim().contains("mult")){
                        countSRStart++;
                    }
                }
            }else{
                if(typeSegReq.toLowerCase().contains("sysrespstartsr")&& !typeSegReq.toLowerCase().trim().contains("mult"))
                {
                    countSRStart++;
                }
            }
        }
        return countSRStart;
    }



    //Method that counts system responses after condition or after scope
    public static int numSREnd(List<SegmentReq> segmentsReq)
    {
        //sysrespStartSR
        ////Scenario  SysRespPos SysRespPos  OR  SysRespPos ConditionPosIncomplete SysRespPos
        //SysRespPosIncomplete
        // SysRespIncompl
        // SysRespIncorrect
        int countSR = 0;
        for(SegmentReq segReq: segmentsReq)
        {
            String typeSegReq = segReq.getTypeOfClause();
            String[] typesSReq = (typeSegReq.trim()).split(" ");
            if(typeSegReq.length() > 1){
                for(String t: typesSReq){
                    if((t.toLowerCase().trim().contains("sysresp") || t.toLowerCase().trim().contains("multsrs")) && !t.toLowerCase().trim().contains("sysrespstartsr")){
                        countSR++;
                    }
                }
            }else{
                if(typeSegReq.toLowerCase().contains("sysrespsr") ||  typeSegReq.toLowerCase().contains("sysresp") ||
                        typeSegReq.toLowerCase().contains("multsrs"))
                {
                    countSR++;
                }
            }
        }
        return countSR;
    }





    //Method that detects if a requirement has more than one system responses
    public static boolean hasMultipleSR(List<SegmentReq> segmentsReq)
    {
        boolean hasMulSR = false;
        for(SegmentReq segReq: segmentsReq){
            if(segReq.getTypeOfClause().toLowerCase().contains("multsr")){
                hasMulSR = true;
            }
        }
        return hasMulSR;
    }
    //Method that detects if a requirement has more than one  condition
    public static boolean hasMultConds(List<SegmentReq> segmentReq){
        boolean hasMulCond = false;
        for(SegmentReq s: segmentReq){
            if(s.getTypeOfClause().toLowerCase().contains("multcond")){
                hasMulCond = true;
            }
        }
        return hasMulCond;
    }
    //Method that counts the number of conditions that are incomplete
    public static int numCondIncomple(List<SegmentReq> segmentsReq)
    {
        int countCondIncomp = 0;
        for (SegmentReq segReq: segmentsReq)
        {
            String typeSegReq = segReq.getTypeOfClause().trim().toLowerCase();
            if(typeSegReq.contains("conditions9")||typeSegReq.contains("conditions10")||
                    typeSegReq.contains("conditions11")||typeSegReq.contains("condposincomplete"))
            {
                countCondIncomp++;
            }
        }
        return  countCondIncomp;
    }

    //Method that counts the number of scopes that are incomplete
    public static int numScopeIncomplete(List<SegmentReq> segmentsReq){
        int scopeIncomp =0;
        for (SegmentReq segReq: segmentsReq)
        {
            String typeSegReq = segReq.getTypeOfClause().trim().toLowerCase();
            if(typeSegReq.contains("scopeposincomplete"))
            {
                scopeIncomp++;
            }
        }
        return scopeIncomp;
    }

    //Method that checks if  a system response has a passive voice
    public static boolean pasVoiceSR(SegmentReq seg)
    {
        boolean hasPasVoiceSR = false;
        if(seg.getPassiveVoiceSR()!= null){
            String typeSegReq = seg.getPassiveVoiceSR().toLowerCase();
            if(typeSegReq.contains("passivevoice") || typeSegReq.contains("passivesr") )
            {
                hasPasVoiceSR = true;
            }
        }
        return hasPasVoiceSR;
    }

    //Method that checks if  a condition has a passive voice
    public static boolean pasVoiceCond(SegmentReq s) {
        boolean hasPasVoiceCond = false;
        if(s.getPassiveVoiceCond()!=null){
            String typeSegReq = s.getPassiveVoiceCond().toLowerCase();
            if (typeSegReq.contains("passivevoice") || typeSegReq.contains("passivecond") ) {
                hasPasVoiceCond = true;
            }
        }

        return hasPasVoiceCond;
    }
    //Method that checks if  a condition does not have an actor
    public static boolean noActorCond(SegmentReq seg)
    {
        boolean hasCondNoActor = false;
        if(seg.getTypeOfClause()!=null){
            if(seg.getTypeOfClause().contains("conditionS9"))
            {
                hasCondNoActor=true;
            }
        }
        return hasCondNoActor;
    }

    //Method that checks if  a condition does not have an actor nor verb
    public static boolean noActorNorVerbCond(SegmentReq seg)
    {
        boolean hasCondNoActorNoVerb = false;
        if(seg.getTypeOfClause()!= null){
            if(seg.getTypeOfClause().contains("conditionS10") ||seg.getTypeOfClause().contains("conditionS11")
                    || seg.getTypeOfClause().toLowerCase().contains("conditionposs10")){
                hasCondNoActorNoVerb = true;
            }
        }
        return hasCondNoActorNoVerb;
    }
    //Method that counts the number of scope in a requirement
    public static int numScopeReq(List<SegmentReq> segmentsReq)
    {
        int countScope = 0;
        for(SegmentReq segReq: segmentsReq)
        {
            String t = segReq.getTypeOfClause();
            String[] types = t.split(" ");
            if(types.length>1){
                for(String type:types){
                    if((type.toLowerCase().trim()).contains("scope")){
                        countScope++;
                    }
                }
            }else{
                if(t.contains("scopeSC1") || t.contains("scopeSC2") || t.contains("scopePos"))
                {
                    countScope++;
                }
            }
        }
        return countScope;
    }

    //Method that checks if a verb belongs to our list of not precise verbs
    public static boolean notPreciseVerb(String w){
        boolean isAmbiguos = false;
        HashMap<String, Integer> listHomonyms = new HashMap<>();
        listHomonyms.put("accomplish",1);//
        listHomonyms.put("account",1);//consider
        listHomonyms.put("come",1);//
        listHomonyms.put("consid",1);//consider
        listHomonyms.put("default",1);
        listHomonyms.put("defin",1);//define
        listHomonyms.put("do",1);
        listHomonyms.put("done",1);
        listHomonyms.put("get",1);
        listHomonyms.put("got",1);
        listHomonyms.put("gotten",1);
        listHomonyms.put("make",1); //make
        listHomonyms.put("perform",1);
        listHomonyms.put("process", 1);
        listHomonyms.put("propos",1);//propose
        listHomonyms.put("rais",1); //raise
        listHomonyms.put("read",1);
        listHomonyms.put("support",1);
        listHomonyms.put("want",1);

        if(listHomonyms.containsKey(w)){
            isAmbiguos = true;
        }
        return isAmbiguos;
    }

    //Method tha checks if at the end of the segment there is an "or"
    public static boolean checkOrAtEndSeg(SegmentReq seg, String cleanSeg){
        boolean hasOrComma = false;
        String textSegment = Utils.getStringFromSegment(seg.getStartSegment(),seg.getEndSegment(), cleanSeg);
        String[] wordsSeg =  textSegment.toLowerCase().split(" ");
        if(wordsSeg.length > 2){
            if(wordsSeg[wordsSeg.length - 1].equals("or") || wordsSeg[wordsSeg.length - 1].contains(",")
                    ||wordsSeg[wordsSeg.length - 1].equals(",or")){
                hasOrComma = true;
            }
        }
        return  hasOrComma;
    }

    //Method tha checks if there is an "or" in  the segment
    public static boolean checkOr(SegmentReq seg, String cleanSeg){
        boolean hasOrComma = false;
        String textSegment = Utils.getStringFromSegment(seg.getStartSegment(),seg.getEndSegment(), cleanSeg);
        String[] wordsSeg =  textSegment.toLowerCase().split(" ");
        if(wordsSeg.length <=3){
            if(wordsSeg[wordsSeg.length - 1].equals("or") || wordsSeg[wordsSeg.length - 1].contains(",")
                    ||wordsSeg[wordsSeg.length - 1].equals(",or")){
                hasOrComma = true;
            }
        }
        return hasOrComma;
    }

    //Get all segments that contain consecutive conditions
    public static List<List<SegmentReq>> getConsecCondSegments(List<SegmentReq> segmentsReq){
        List<List<SegmentReq>> consecCondSeg = new ArrayList<>();
        List<Integer> indexConsec = new ArrayList<>();

        for(int i=0; i < segmentsReq.size(); i++){
            if(segmentsReq.get(i).getTypeOfClause().toLowerCase().contains("cond")){
                indexConsec.add(i);
            }
        }
        int start = -1;
        int end = -1;
        List<ConsecCond> conds = new ArrayList<>();
        for(int j=0; j< indexConsec.size(); j++){

            if(j+1 < indexConsec.size()){
                if(indexConsec.get(j) - indexConsec.get(j+1) == -1){
                    if(start==-1 && end==-1){
                        start = indexConsec.get(j);
                        end = indexConsec.get(j+1);
                    }else if(end == indexConsec.get(j)) {
                        end = indexConsec.get(j+1);
                    }
                }else {
                    if(start!=-1 && end!=-1){
                        ConsecCond c = new ConsecCond(start, end);
                        conds.add(c);
                        start =-1;
                        end = -1;
                    }
                }
            }
            else{
                if(start!=-1 && end!=-1){
                    ConsecCond c = new ConsecCond(start, end);
                    conds.add(c);
                    start =-1;
                    end = -1;
                }
            }
        }

        for(ConsecCond condition: conds){
            List<SegmentReq> temp = new ArrayList<>();
            for(int i = condition.getStart(); i <= condition.getEnd(); i++){
                temp.add(segmentsReq.get(i));
            }
            consecCondSeg.add(temp);
        }

        return consecCondSeg;
    }

    //Method that returns true if the last segment of the requirement is a condition
    public static boolean lastSegmentIsCondition(List<SegmentReq> segmentsReq){
        boolean lastIsCond = false;
        if(segmentsReq.get(segmentsReq.size() - 1).getTypeOfClause().contains("cond")){
            lastIsCond = true;
        }
        return lastIsCond;
    }

    //returns the index/position within the requirement of the given condition
    public static int findIndxLastCond(List<SegmentReq> segConds){
        int lastCond = -1;
        for(int i=(segConds.size()-1); i>=0;i--){
            if(segConds.get(i).getTypeOfClause().contains("cond"))
            {
                lastCond = i;
                break;
            }
        }
        return lastCond;
    }

    //Checks for coordination ambiguity
    public static boolean findCoordAmbInSegment(List<SegmentReq> consecConds, int posLastCond, String cleanSeg){
        boolean hasCoordAmb = false;
        for(int i=0; i < posLastCond ; i++){
            if(consecConds.get(i).getTypeOfClause().contains("cond")){
                if(checkOrAtEndSeg(consecConds.get(i),cleanSeg)){
                    hasCoordAmb = true;
                }
            }else{
                if(checkOr(consecConds.get(i),cleanSeg)){
                    hasCoordAmb=true;
                }
            }
        }
        return hasCoordAmb;
    }


    //This method checks for coordination ambiguity using the results of the Tregex patterns
     /* Coordination ambiguity refers to a requirement that has two or more conditions and these
       conditions are connected by a coordinated conjunction “or”*/
    public static boolean checkCoordAmbCondTregex(List<SegmentReq> segmentsReq, String reqId, String cleanSeg)
    {
        boolean hasCoordAmb = false;
        List<List<SegmentReq>> consecConds = getConsecCondSegments(segmentsReq);

        for(List<SegmentReq> s: consecConds){
            if(numCondBefore(s) > 1){ //The requirement has more than one condition
                //Scenario 1: last is a condition
                if(lastSegmentIsCondition(s)){
                    hasCoordAmb= findCoordAmbInSegment(s,(consecConds.size()-1),cleanSeg);
                }else{//Scenario 2: last is not a condition
                    int indexLastCond = findIndxLastCond(s);
                    hasCoordAmb= findCoordAmbInSegment(s,(indexLastCond+1), cleanSeg);
                }
            }
        }

        return hasCoordAmb;
    }

    //check if the word is "and"
    public static boolean isAnd(String word){
        boolean isAnd = false;
        if(word.trim().length() == 3 && word.toLowerCase().contains("and")){
            isAnd = true;
        }
        return isAnd;
    }

    public static boolean isOr(String word){
        boolean isOr = false;
        if(word.trim().length() == 2 && word.toLowerCase().contains("or")){
            isOr = true;
        }
        return isOr;
    }



    public static boolean hasMultCondConsec(String t){
        boolean consecConds = false;
        String[] types = t.split(" ");
        ArrayList<Integer> conds = new ArrayList<>();
        if(types.length>1){
            for(int i=0; i< types.length; i++){
                if(types[i].contains("cond")){
                    conds.add(i);
                }
            }
        }

        for(int i=1;i<conds.size();i++)
        {
            if((conds.get(i) - conds.get(i-1)) == 1)
            {
                consecConds = true;
                break;
            }
        }

        return consecConds;
    }


    /* Coordination ambiguity refers to a requirement that has two or more conditions and these
      conditions are connected by a coordinated conjunction “or”*/
    public static boolean checkCoordAmbCondPos(List<SegmentReq> segmentsReq, String reqId, String cleanSeg, MaxentTagger t){
        boolean hasCoordAmb = false;
        boolean isAdditionalInfor = false;
        for(SegmentReq s: segmentsReq){

            //hasMultCondConsec Assumming that in the label it has condS1 condS8
            if(s.getTypeOfClause().toLowerCase().contains("multcond") || hasMultCondConsec(s.getTypeOfClause().toLowerCase())){
                ArrayList<TaggedWord> sentenceTagger = PosMethods.getTagSentence(Utils.getStringFromSegment(s.getStartSegment(),s.getEndSegment(),cleanSeg),t);
                ArrayList<Condition> segments = new ArrayList<>();
                ArrayList<Condition> segmentsBullets = PosMethodsCondition.detectMultCondReq(sentenceTagger);

                if(segmentsBullets.size() > 2 ){
                    segments = new ArrayList<>(segmentsBullets);
                    ArrayList<TaggedWord> condsBef = segments.get(0).getConditionTagged();
                    if(condsBef.get(0).word().toLowerCase().contains("i.e.")){
                        isAdditionalInfor = true;
                    }
                    if(condsBef.get(0).word().toLowerCase().contains("e.g.")){
                        isAdditionalInfor = true;
                    }
                }
                int countCond = 0;
                if(!isAdditionalInfor){
                    for(int j=0; j< segments.size(); j++){
                        String typeSingleCond = PosMethodsCondition.getSingleCondBulletType(segments.get(j));

                        if(PosMethods.hasSubConj(segments.get(j).getConditionTagged()) || typeSingleCond.toLowerCase().contains("completecond") ||
                                typeSingleCond.toLowerCase().contains("incomplcond") ){
                            // ArrayList<TaggedWord> c: segments

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



                            //or if | , if  |,or if  | , And If
                            if(last.equals("or") || last.contains(",or") ){
                                if(j+1 < segments.size()){
                                    if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged()) ||
                                            PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond") ){
                                        hasCoordAmb = true;
                                    }
                                }
                            }else if(last.contains(",") && befLast.toLowerCase().equals("or")){
                                if(j+1 < segments.size()){
                                    if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged()) ||
                                            PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond")){
                                        hasCoordAmb = true;
                                    }
                                }
                            }else if(firstWord.equals("or")|| firstWord.contains(",or")){
                                if(j+1 < segments.size()) {
                                    if (PosMethods.hasSubConj(segments.get(j + 1).getConditionTagged()) ||
                                            PosMethodsCondition.getSingleCondBulletType(segments.get(j + 1)).toLowerCase().contains("cond")) {
                                        hasCoordAmb = true;
                                    }
                                }
                            }else if(secWord.equals("or")||secWord.equals(",or")){
                                if(j+1 < segments.size()) {
                                    if (PosMethods.hasSubConj(segments.get(j + 1).getConditionTagged()) ||
                                            PosMethodsCondition.getSingleCondBulletType(segments.get(j + 1)).toLowerCase().contains("cond")) {
                                        hasCoordAmb = true;
                                    }
                                }
                            }

                            else if(( last.trim().length()>2 && !isAnd(last)) && befLast.toLowerCase().equals("or")){
                                if(j+1 < segments.size()){
                                    if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged()) ||
                                            PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond")){
                                        hasCoordAmb = true;
                                    }
                                }
                            }else if( (last.trim().length()>2 && !isAnd(last)) && befLast.toLowerCase().equals(",or")){
                                if(j+1 < segments.size()){
                                    if(PosMethods.hasSubConj(segments.get(j+1).getConditionTagged()) ||
                                            PosMethodsCondition.getSingleCondBulletType(segments.get(j+1)).toLowerCase().contains("cond")){
                                        hasCoordAmb = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return hasCoordAmb;
    }

    public static boolean typeHasCoordAmb(List<SegmentReq> segmentsReq){
        boolean hasCoord = false;
        for(SegmentReq s:segmentsReq){
            if(s.getTypeOfClause().toLowerCase().contains("coordamb")){
                hasCoord = true;
                break;
            }
        }
        return hasCoord;
    }



    public static boolean isSingleCondition(String type){
        boolean singleCondition = false;
        String[] types = type.split(" ");
        if(types.length > 1){
            singleCondition = false;
        }else if(types.length == 1){
            if(types[0].contains("srs") || types[0].contains("scope") || types[0].contains("mult")){
                singleCondition = false;
            }else if(types[0].contains("cond") ){
                singleCondition = true;
            }
        }
        return  singleCondition;
    }



    /*Method that checks for smells at requirement level, i.e., non atomic, incomplete req, incorrect order req,
     not a req and coordination ambiguity. To do so this method uses the methods defined in this class
     */
    public static SmellReq identifySmellsReqLevel(List<SegmentReq> segmentsReq, String reqId, String cleanReq, MaxentTagger t, String patternMatched)
    {
        //Atomic Requirements
        SmellReq req = new SmellReq(reqId);
        if(numSR(segmentsReq) > 1 || hasMultipleSR(segmentsReq))
        {
            req.setNonAtomic(true);

        }
        //Incomplete requirement
        if( (numCondAfter(segmentsReq) > 0 || numCondBefore(segmentsReq) > 0 || numCondIncomple(segmentsReq) >0 ||
                numScopeReq(segmentsReq) > 0 || numScopeIncomplete(segmentsReq) > 0 || numCondNoSR(segmentsReq) > 0) && (numSR(segmentsReq) == 0)
                && !hasMultipleSR(segmentsReq) )
        {
            req.setIncompReq(true);
        }
        //Incorrect order of parts in the requirement

        if(numCondAfter(segmentsReq) > 0  && numSR(segmentsReq) > 0)
        {
            req.setUnorderCondReq(true);
        }

        //Coordination Ambiguity using the both methods
        if(typeHasCoordAmb(segmentsReq)){
            req.setCoordAmbCommaOr(true);

        }else if(checkCoordAmbCondTregex(segmentsReq,reqId,cleanReq) || checkCoordAmbCondPos(segmentsReq,reqId,cleanReq,t)){
            req.setCoordAmbCommaOr(true);
        }
        //Not requirement
        if((numCondAfter(segmentsReq) == 0 && numCondBefore(segmentsReq) == 0 && numCondIncomple(segmentsReq) ==0)
                && numSR(segmentsReq) == 0 && !hasMultConds(segmentsReq) && numCondNoSR(segmentsReq) == 0
                && !hasMultipleSR(segmentsReq))
        {
            req.setNotRequirement(true);
        }
        return req;
    }


    /* This method checks for smells: incomplete Condition and System response, not precise verbs, and passive voice.
     To do so this method uses the methods defined in this class
    * */
    public static SegmentReq identifySmellSeg(SegmentReq segmentsReq, String reqId, String cleanReq , MaxentTagger tagger)
    {
        SmellSegment reqSegError = new SmellSegment(reqId);
        if(segmentsReq.getTypeOfClause().toLowerCase().contains("incompl")){
            //condPosIncomplNoVerb
            String typeClause = segmentsReq.getTypeOfClause().toLowerCase();
            if(typeClause.contains("scopeincompl")){
                reqSegError.setNoNounScope(true);
            }
            if(typeClause.contains("sysrespincomplnoverb")){
                reqSegError.setNoVerbSR(true);
            }
            if(typeClause.contains("sysrespincomplnomodal")){
                reqSegError.setNoModalSR(true);
            }
            if(typeClause.contains("sysrespincomplnomodalnoverb")){
                reqSegError.setNoModalSR(true);
                reqSegError.setNoVerbSR(true);
            }
            if(typeClause.contains("condposincomplnonoun")){
                reqSegError.setNoActorCond(true);
            }
            if(typeClause.contains("condposincomplnoverb")  ){


                reqSegError.setNoVerbCond(true);
            }
            if(typeClause.contains("condposincomplnonounnoverb")){
                reqSegError.setNoActorCond(true);
                reqSegError.setNoVerbCond(true);
            }
        }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("conditionwithsymbol")){
            reqSegError.setNoVerbCond(true);
            // multcondposwithnoverb,multsrspos condposincomplnoverb
        }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("condcontwithnoverb")){
            reqSegError.setNoVerbCond(true);

        }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("multcond")){
            // sysrespposwithambverb multcondpos, conditionposs10 multcondposwithnoverb
            for(String t: segmentsReq.getTypeOfClause().split(" ")){

                if(t.toLowerCase().contains("multcond") && t.toLowerCase().contains("noverb")){
                    reqSegError.setNoVerbCond(true);
                }
                if(t.toLowerCase().contains("multcond") && t.toLowerCase().contains("passive")){
                    reqSegError.setpVoiceCond(true);
                }
                if(t.toLowerCase().contains("multcond") && t.toLowerCase().contains("ambverb")){
                    reqSegError.setAmbVerbCond(true);
                }
                if(segmentsReq.getTypeOfClause().toLowerCase().contains("conditionposs10")){
                    reqSegError.setNoVerbCond(true);
                    reqSegError.setNoActorCond(true);
                }
            }

        }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("conditionposs10")){
            reqSegError.setNoVerbCond(true);
            reqSegError.setNoActorCond(true);
            //conditionposs10
        }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("cond") &&
                !segmentsReq.getTypeOfClause().toLowerCase().contains("conditions12") ){

            if(segmentsReq.getVerbCondition()==null || segmentsReq.getVerbCondition().length() == 0 ) {

                String segCondText = Utils.getStringFromSegment(segmentsReq.getStartSegment(), segmentsReq.getEndSegment(),cleanReq);
                ArrayList<TaggedWord> condTag = PosMethods.getTagSentence(segCondText,tagger);

                if(PosMethodsCondition.noVerbCond(condTag)){

                    reqSegError.setNoVerbCond(true);
                }

            }else{
                if((segmentsReq.getVerbCondition().trim()).length()==0){

                    reqSegError.setNoVerbCond(true);
                }else{
                    //there is a verb
                    String segCondText = Utils.getStringFromSegment(segmentsReq.getStartSegment(), segmentsReq.getEndSegment(),cleanReq);
                    ArrayList<TaggedWord> condTag = PosMethods.getTagSentence(segCondText,tagger);

                    if(PosMethods.isPassiveVoice(condTag)){

                        reqSegError.setpVoiceCond(true);
                    }else{
                        //Not a passive voice but we need to extract the correct verb

                        int posVerb = PosMethodsCondition.getVerbInCond(condTag);
                        if(posVerb!=-1){
                            if(condTag.get(posVerb).word().toLowerCase().contains("has") || condTag.get(posVerb).word().toLowerCase().contains("have")){
                                if(posVerb+1 <= condTag.size()){
                                    if(condTag.get(posVerb+1).tag().toLowerCase().contains("vb")){
                                        segmentsReq.setVerbCondition(condTag.get(posVerb+1).word());

                                    }
                                }
                            }

                        }
                    }

                }
                if(segmentsReq.getPassiveVoiceCond().trim().toLowerCase().contains("passivecond")||
                        segmentsReq.getPassiveVoiceCond().trim().toLowerCase().contains("passivevoice")){
                    reqSegError.setpVoiceCond(true);
                }
            }

        }
        //
        if(isSingleCondition(segmentsReq.getTypeOfClause().toLowerCase()) &&
                !segmentsReq.getTypeOfClause().toLowerCase().contains("conditions12")){
            // Check if type is conditionS9
            if (noActorCond(segmentsReq)){
                reqSegError.setNoActorCond(true);
            }
            // Check if type is either conditionS10 or conditionS11
            if (noActorNorVerbCond(segmentsReq)){
                reqSegError.setNoVerbCond(true);
                reqSegError.setNoActorCond(true);
            }
            //Check if the type is Passive Voice

            if (pasVoiceCond(segmentsReq))//pVoiceCondMsg also added to be recognized for POS
            {

                reqSegError.setpVoiceCond(true);
            }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("passivecond")){

                segmentsReq.setPassiveVoiceCond("PassiveVoice");
                reqSegError.setpVoiceCond(true);

            }else{
                //if the type does not contain passive voice
                if(segmentsReq.getTypeOfClause().toLowerCase().contains("cond") ){

                    String segCondText = Utils.getStringFromSegment(segmentsReq.getStartSegment(), segmentsReq.getEndSegment(),cleanReq);
                    ArrayList<TaggedWord> condTag = PosMethods.getTagSentence(segCondText,tagger);
                    if(PosMethods.isPassiveVoice(condTag)){
                        String mainVerb = PosMethods.getVerbPassiveVoice(condTag);
                        segmentsReq.setPassiveVoiceCond("PassiveVoice");
                        segmentsReq.setVerbCondition(mainVerb);
                        reqSegError.setpVoiceCond(true);
                        //The verb was identified after detecting  a passive voice
                        reqSegError.setNoVerbCond(false);

                    }
                }
            }

        }

        if (pasVoiceSR(segmentsReq))
        {
            reqSegError.setpVoiceSR(true);
        }else if (segmentsReq.getTypeOfClause().toLowerCase().contains("passivesr")){
            segmentsReq.setPassiveVoiceSR("PassiveVoice");
            reqSegError.setpVoiceSR(true);

        }else{
            if(segmentsReq.getTypeOfClause().toLowerCase().contains("sysresp")){
                String segCondText = Utils.getStringFromSegment(segmentsReq.getStartSegment(), segmentsReq.getEndSegment(),cleanReq);
                ArrayList<TaggedWord> sysRespTag = PosMethods.getTagSentence(segCondText,tagger);
                if(PosMethods.isPassiveVoiceSr(sysRespTag)){
                    segmentsReq.setPassiveVoiceSR("PassiveVoice");
                    String mainVerb = PosMethods.getVerbPassiveVoiceSr(sysRespTag);
                    segmentsReq.setSrVerb(mainVerb);
                    reqSegError.setpVoiceSR(true);
                    //The passive voice has a verb
                    reqSegError.setNoVerbSR(false);
                }
            }
        }
        //Smells for not precise verb
        if(segmentsReq.getVerbCondition()!= null){

            String stemmedVerb = Utils.wordStemmer(segmentsReq.getVerbCondition());

            if(notPreciseVerb(stemmedVerb)){
                reqSegError.setAmbVerbCond(true);
            }
        }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("ambverb") &&
                segmentsReq.getTypeOfClause().toLowerCase().contains("cond")){
            //// sysrespposwithambverb conditionwithmodal
            for(String t:segmentsReq.getTypeOfClause().split(" ")){
                if(t.toLowerCase().contains("ambverb") && t.toLowerCase().contains("cond")){
                    reqSegError.setAmbVerbCond(true);
                }
            }
        }
        if(segmentsReq.getSrVerb() != null){
            String stemmedVerb = Utils.wordStemmer(segmentsReq.getSrVerb());
            if(notPreciseVerb(stemmedVerb)){
                reqSegError.setAmbVerbSR(true);
            }
        }else if(segmentsReq.getTypeOfClause().toLowerCase().contains("ambverb") &&
                (segmentsReq.getTypeOfClause().toLowerCase().contains("sysresp") ||
                        segmentsReq.getTypeOfClause().toLowerCase().contains("srs"))){
            for(String t:segmentsReq.getTypeOfClause().split(" ")){
                if(t.toLowerCase().contains("ambverb") &&
                        (t.toLowerCase().contains("sysresp") ||
                                t.toLowerCase().contains("srs"))){
                    reqSegError.setAmbVerbSR(true);
                }
            }
        }

        segmentsReq.setErrorsSegment(reqSegError);
        return segmentsReq;
    }
}


