import com.opencsv.CSVWriter;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/*
* File tha contains the methods to generate the output file
* */
public class Output {


    public static ArrayList<Integer> createCsvClauses(List<Requirement> requirements, String sraDocument,  Boolean smells, Boolean recomPattern, MaxentTagger tagger)
    {
        ArrayList<Integer> wordsReq = new ArrayList<Integer>();

        try {
            // An array per row
            Writer fileWriter = new FileWriter(sraDocument+".csv", false);
            CSVWriter csvWriter = new CSVWriter(fileWriter,
                    ';',
                    CSVWriter.DEFAULT_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);


            if(recomPattern && smells){
                csvWriter.writeNext(new String[]{"Req ID", "Segment ID","Req Segment", "Non-atomic requirement", "Incomplete requirement",
                        "Incorrect order requirement", "Coordination ambiguity",  "Not requirement","Incomplete condition",
                        "Incomplete system response", "Passive voice", "Not precise verb", "Rimay Pattern "});
                //13 Fields
            }



            Integer clauseCounter = 0;
            for(Requirement req : requirements)
            {
                Integer numberWordsSra = (req.getPreprocessedReq().split(" ")).length;
                wordsReq.add(numberWordsSra);


                if(req.getOrderSegs() != null )
                {
                    //Generate the labels for the fields as REQUIREMENT levels
                    ArrayList<String> listConditions = Utils.getVerbsReq(req, tagger, req.getReqId());
                    ArrayList<SegmentReq> timeConds = Utils.getCondTimes(req);
                    ArrayList<String> listScopes = Utils.getScopeReq(req);
                    String typeRimayPattern =  Utils.getTypePatternRimayCsv(req.getPatternMatched(),listConditions,listScopes, req.getReqId(),timeConds );
                    //Getting the smells for the requirement at requirement level
                    SmellReq errorsReq = req.getSmellsReq();
                    Integer counterClausesInternal = 0;

                    for(SegmentReq clause : req.getOrderSegs())
                    {

                        counterClausesInternal +=1;
                        clauseCounter +=1;
                        Integer numberWords = (req.getPreprocessedReq().split(" ")).length; //
                        ArrayList<String> reqLine = new ArrayList<String>();
                        //Errors segment level
                        SmellSegment errorSeg = clause.getErrorsSegment();
                        reqLine.add(req.getReqId()); //Req ID   #1
                        reqLine.add(req.getReqId()+"_"+clauseCounter.toString()); //Clause counter  #2
                        reqLine.add(Utils.getStringFromSegment(clause.getStartSegment(), clause.getEndSegment(), req.getPreprocessedReq())); //Clause text   #3


                        if(recomPattern && smells)
                        {
                            //Generate the labels for the fields as SEGMENT levels
                            String typeClause = clause.getTypeOfClause().toLowerCase();

                            //Getting the concepts from the requirements
                            //Concepts for condition
                            String actorCondition = "";
                            String verbCondition  = "";
                            //Concepts for system response
                            String srActor = "";
                            String srModal = "";
                            String srVerb = "";
                            String pennTreeBankVerbCondition = "";
                            String verbCondInc = "";
                            String pennTreeBankVerbSR = "";

                            String nonAtomic = "";
                            String incompReq = "";
                            String unOrdReq = "";
                            String coordAmb = "";
                            String notReq = "";
                            //Smells segments of requirement
//                            String incompleteCond = "";
                            String noActorCond= "";
                            String noVerbCond= "";
//                            String incompleteSR = "";
                            String noActorSR = "";
                            String noModalSR= "";
                            String noVerbSR= "";
//                            String noNounScope= "";

                            String pVoiceCond= "";
                            String pVoiceSR= "";
                            String ambVerbCond= "";
                            String ambVerbSR= "";

                            // We are retrieving all the concepts but we need to fill out the ones related to the other ones
                            if(typeClause.contains("condition"))
                            {

                                if (clause.getActorCondition()!= null)
                                {
                                    actorCondition = clause.getActorCondition();
                                }
                                if (clause.getVerbCondition() != null)
                                {
                                    verbCondition = clause.getVerbCondition();
                                }
                                if(clause.getPennTreeBankVerbCondition() != null)
                                {
                                    pennTreeBankVerbCondition = clause.getPennTreeBankVerbCondition();
                                }
                                if(clause.getVerbCondInc() != null)
                                {
                                    verbCondInc = clause.getVerbCondInc();
                                }

                            }

                            if (typeClause.contains("sysresp"))
                            {

                                if(clause.getSrActor() != null)
                                {
                                    srActor = clause.getSrActor();
                                }
                                if(clause.getSrModal() != null)
                                {
                                    srModal = clause.getSrModal();
                                }
                                if(clause.getSrVerb()!= null)
                                {
                                    srVerb = clause.getSrVerb();
                                }
                                if(clause.getPennTreeBankVerbSR()!=null)
                                {
                                    pennTreeBankVerbSR = clause.getPennTreeBankVerbSR();
                                }
                            }
                            String verbConditionReq = PosMethods.getTypeConditionCsv(verbCondition, pennTreeBankVerbCondition);
                            String conditionTypeCsv = "";
                            if(verbConditionReq.length()>1)
                            {
                                if(verbConditionReq.equals("logicalExpression"))
                                {
                                    conditionTypeCsv = "Logical Expression";

                                }else if (verbConditionReq.equals("trigger"))
                                {
                                    conditionTypeCsv = "Trigger";
                                }else
                                {
                                    conditionTypeCsv = "Others";
                                }
                            }

                            if(errorsReq.getNonAtomic())
                            {
                                nonAtomic = errorsReq.getNonAtomicMsg();
//                                nonAtomic = "1";
                            }
                            if(errorsReq.getIncompReq())
                            {
                                incompReq = errorsReq.getIncompReqMsg();
//                                incompReq = "1";
                            }
                            if(errorsReq.getUnorderCondReq())
                            {
                                unOrdReq = errorsReq.getUnorderCondReqMsg();
//                                unOrdReq = "1";
                            }
                            if(errorsReq.getNotRequirement())
                            {
                                notReq = errorsReq.getNotReqMsg();
//                                notReq = "1";
                            }
                            if(errorsReq.getCoordAmbCommaOr()){

                                coordAmb = errorsReq.getCoordAmbCommaOrMsg();
//                                coordAmb = "1";
                            }
                            if(errorSeg.getNoActorCond()){
                                noActorCond = errorSeg.getNoActorCondMsg();
//                                noActorCond = "1";
                            }
                            if(errorSeg.getNoVerbCond()){
                                noVerbCond = errorSeg.getNoVerbCondMsg();
//                                noVerbCond = "1";
                            }
                            if(errorSeg.getNoActorSR()){
                                noActorSR = errorSeg.getNoActorSRMsg();
//                                noActorSR = "1";
                            }
                            if(errorSeg.getNoModalSR()){
                                noModalSR = errorSeg.getNoModalSRMsg();
//                                noModalSR = "1";
                            }
                            if(errorSeg.getNoVerbSR()){
                                noVerbSR = errorSeg.getNoVerbSRMsg();
//                                noVerbSR = "1";
                            }
//                            if(errorSeg.getNoNounScope()){
////                                noNounScope = errorSeg.getNoNounScopeMsg();
//                                noNounScope = "1";
//                            }
                            if(errorSeg.getPVoiceCond()){
                                pVoiceCond = errorSeg.getpVoiceCondMsg();
//                                pVoiceCond = "1";
                            }
                            if(errorSeg.getPVoiceSR()){
                                pVoiceSR = errorSeg.getpVoiceSRMsg();
//                                pVoiceSR = "1";
                            }

                            if(errorSeg.getAmbVerbCond()){

                                ambVerbCond = errorSeg.getAmbVerbCondMsg();
//                                ambVerbCond = "1";
                            }
                            if(errorSeg.getAmbVerbSR()){
                                ambVerbSR = errorSeg.getAmbVerbSRMsg();
//                                ambVerbSR = "1";
                            }

                            if(counterClausesInternal==1)
                            {
                                reqLine.add(nonAtomic);//4 NonAtomic
                                reqLine.add(incompReq);//5 IncompleteReq
                                reqLine.add(unOrdReq);//6 UnorderedReq
                                reqLine.add(coordAmb);//7 CoordAmb
                                reqLine.add(notReq);//8 NotReq
                            }else
                            {
                                reqLine.add("");//4 NonAtomic
                                reqLine.add("");//5 IncompleteReq
                                reqLine.add("");//6 UnorderedReq
                                reqLine.add("");//7 CoordAmb
                                reqLine.add("");//8 NotReq
                            }




                            reqLine.add(noActorCond+ " "+noVerbCond); //9
                            reqLine.add(noActorSR+ " "+noModalSR+" "+ noVerbSR); //10
                            reqLine.add(pVoiceCond+ " "+pVoiceSR);//11
                            reqLine.add(ambVerbCond+ " "+ambVerbSR );//12

                            if(counterClausesInternal==1)
                            {
                                reqLine.add(typeRimayPattern);   // 13
                                switch (typeRimayPattern){

                                    case "1. Scope and SR":
                                        reqLine.add("For each|all|... \"Text\" ,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "2. Scope, condition(Precondition LE), and SR":
                                        reqLine.add("For each|all|... \"Text\", if <Property> is equal to | is less or equal to |... \"Value\" ,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "3. Scope, condition(Trigger), and SR":
                                        reqLine.add("For each|all|... \"Text\", when the? Actor <Action> (every \"Frequency\")?,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "4. Scope, condition (Time Adverb) and SR":
                                        reqLine.add("For each|all|... \"Text\", after|before \"Text\" ,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "5. System response":
                                        reqLine.add("The? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "6. Condition(Precondition LE), and SR":
                                        reqLine.add("If <Property> is equal to | is less or equal to |... \"Value\" ,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "7. Condition(Trigger), and SR":
                                        reqLine.add("When the? Actor <Action> (every \"Frequency\")? ,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "8. Condition (Time Adverb) and SR":
                                        reqLine.add("After|Before \"Text\" ,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "9. Scope, multiple conditions, and SR":
                                        reqLine.add("For each|all|... \"Text\", if <Property> is equal to | is less or equal to |... \"Value\" ,|and  when the? Actor <Action> (every \"Frequency\")? ,|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    case "10. Multiple conditions, and SR":
                                        reqLine.add("If <Property> is equal to | is less or equal to |... \"Value\" ,|and  when the? Actor <Action> (every \"Frequency\"),|then the? Actor must <Action> (every \"Text\")?.");
                                        break;
                                    default:
                                        reqLine.add("");   // 14


                                }

                            }
                            else
                            {
                                reqLine.add("");   // 13
                                reqLine.add("");   // 14
                            }


                        }




                        csvWriter.writeNext(reqLine.toArray(new String[0]));
                    }
                }else
                {
                    clauseCounter +=1;
                    ArrayList<String> reqLine = new ArrayList<String>();
                    reqLine.add(req.getReqId());   //#1
                    reqLine.add(req.getReqId()+"_"+clauseCounter.toString());//#2
                    reqLine.add(req.getPreprocessedReq());  //#3
                    if(recomPattern && smells)
                    {
                        reqLine.add(" ");   //  #4
                        reqLine.add(" ");   //  #5
                        reqLine.add(" ");   //  #6
                        reqLine.add(" ");   //  #7
                        reqLine.add(" ");   //  #8
                        reqLine.add(" ");   //  #9
                        reqLine.add(" ");   //  #10
                        reqLine.add(" ");   //  #11
                        reqLine.add(" ");   //  #12
                        reqLine.add(" ");   //  #13
                        reqLine.add(" ");   //  #14
                    }
                    csvWriter.writeNext(reqLine.toArray(new String[0]));
                }
            }
            csvWriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return wordsReq;

    }

    public static ArrayList<Integer> createCsvClausesExperiment(List<Requirement> requirements, String sraDocument,  Boolean smells, Boolean recomPattern, MaxentTagger tagger)
    {
        ArrayList<Integer> wordsReq = new ArrayList<Integer>();

        try {
            // An array per row
            Writer fileWriter = new FileWriter(sraDocument+".csv", false);
            CSVWriter csvWriter = new CSVWriter(fileWriter,
                    ';',
                    CSVWriter.DEFAULT_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);


            if(smells){
                csvWriter.writeNext(new String[]{"Req ID", "Segment ID","Req Segment", "Non-atomic requirement", "Incomplete requirement",
                        "Incorrect order requirement", "Coordination ambiguity",  "Not requirement","Incomplete condition",
                        "Incomplete system response", "Passive voice", "Not precise verb", "Rimay Pattern "});
                //13 Fields
            }else if(recomPattern){
                /*0	1	2	3	4	5	6
10	1	Oxygen must provide the user with the ability to access a CBL Account via an ‘ Account Assistance ’ screen	sysrespstartsr			5. System response*/
                csvWriter.writeNext(new String[]{"Req ID", "Segment ID","Req Segment", "03", "04", "05", "Rimay Pattern"});
                //13 Fields

            }



            Integer clauseCounter = 0;
            for(Requirement req : requirements)
            {
                Integer numberWordsSra = (req.getPreprocessedReq().split(" ")).length;
                wordsReq.add(numberWordsSra);


                if(req.getOrderSegs() != null )
                {
                    //Generate the labels for the fields as REQUIREMENT levels
                    ArrayList<String> listConditions = Utils.getVerbsReq(req, tagger, req.getReqId());
                    ArrayList<SegmentReq> timeConds = Utils.getCondTimes(req);
                    ArrayList<String> listScopes = Utils.getScopeReq(req);
                    String typeRimayPattern =  Utils.getTypePatternRimayCsv(req.getPatternMatched(),listConditions,listScopes, req.getReqId(),timeConds );
                    //Getting the smells for the requirement at requirement level
                    SmellReq errorsReq = req.getSmellsReq();
                    Integer counterClausesInternal = 0;

                    for(SegmentReq clause : req.getOrderSegs())
                    {

                        counterClausesInternal +=1;
                        clauseCounter +=1;
                        Integer numberWords = (req.getPreprocessedReq().split(" ")).length; //
                        ArrayList<String> reqLine = new ArrayList<String>();
                        //Errors segment level
                        SmellSegment errorSeg = clause.getErrorsSegment();
                        reqLine.add(req.getReqId()); //Req ID   #[0] 1
                        reqLine.add(req.getReqId()+"_"+clauseCounter.toString()); //Clause counter  #[1] 2
                        reqLine.add(Utils.getStringFromSegment(clause.getStartSegment(), clause.getEndSegment(), req.getPreprocessedReq())); //Clause text   #[2] 3

                        if(recomPattern)
                        {
                            if(counterClausesInternal==1)
                            {
                                reqLine.add(""); //[3]
                                reqLine.add(""); //[4]
                                reqLine.add(""); //[5]
                                reqLine.add(typeRimayPattern);   //[6]
                            }
                            else
                            {
                                reqLine.add("");  // [3]
                                reqLine.add("");  // [4]
                                reqLine.add("");  // [5]
                                reqLine.add("");  // [6]
                            }
                        }



                        if(smells)
                        {
                            //Generate the labels for the fields as SEGMENT levels
                            String typeClause = clause.getTypeOfClause().toLowerCase();

                            //Getting the concepts from the requirements
                            //Concepts for condition
                            String actorCondition = "";
                            String verbCondition  = "";
                            //Concepts for system response
                            String srActor = "";
                            String srModal = "";
                            String srVerb = "";
                            String pennTreeBankVerbCondition = "";
                            String verbCondInc = "";
                            String pennTreeBankVerbSR = "";

                            String nonAtomic = "";
                            String incompReq = "";
                            String unOrdReq = "";
                            String coordAmb = "";
                            String notReq = "";
                            //Smells segments of requirement
//                            String incompleteCond = "";
                            String noActorCond= "";
                            String noVerbCond= "";
//                            String incompleteSR = "";
                            String noActorSR = "";
                            String noModalSR= "";
                            String noVerbSR= "";
//                            String noNounScope= "";

                            String pVoiceCond= "";
                            String pVoiceSR= "";
                            String ambVerbCond= "";
                            String ambVerbSR= "";

                            // We are retrieving all the concepts but we need to fill out the ones related to the other ones



                            String verbConditionReq = PosMethods.getTypeConditionCsv(verbCondition, pennTreeBankVerbCondition);
                            String conditionTypeCsv = "";


                            if(errorsReq.getNonAtomic())
                            {
                               // nonAtomic = errorsReq.getNonAtomicMsg();
                                nonAtomic = "1";
                            }
                            if(errorsReq.getIncompReq())
                            {
                                //incompReq = errorsReq.getIncompReqMsg();
                                incompReq = "2";
                            }
                            if(errorsReq.getUnorderCondReq())
                            {
                               // unOrdReq = errorsReq.getUnorderCondReqMsg();
                                unOrdReq = "3";
                            }
                            if(errorsReq.getCoordAmbCommaOr()){

                                //coordAmb = errorsReq.getCoordAmbCommaOrMsg();
                               coordAmb = "4";
                            }
                            if(errorsReq.getNotRequirement())
                            {
                                notReq = errorsReq.getNotReqMsg();
                                notReq = "5";
                            }

                            if(errorSeg.getNoActorCond()){
//                                noActorCond = errorSeg.getNoActorCondMsg();
                                noActorCond = "6";
                            }
                            if(errorSeg.getNoVerbCond()){
//                                noVerbCond = errorSeg.getNoVerbCondMsg();
                                noVerbCond = "6";
                            }
                            if(errorSeg.getNoActorSR()){
//                                noActorSR = errorSeg.getNoActorSRMsg();
                                noActorSR = "7";
                            }
                            if(errorSeg.getNoModalSR()){
//                                noModalSR = errorSeg.getNoModalSRMsg();
                                noModalSR = "7";
                            }
                            if(errorSeg.getNoVerbSR()){
//                                noVerbSR = errorSeg.getNoVerbSRMsg();
                                noVerbSR = "7";
                            }
//                            if(errorSeg.getNoNounScope()){
////                                noNounScope = errorSeg.getNoNounScopeMsg();
//                                noNounScope = "1";
//                            }
                            if(errorSeg.getPVoiceCond()){
//                                pVoiceCond = errorSeg.getpVoiceCondMsg();
                                pVoiceCond = "9";
                            }
                            if(errorSeg.getPVoiceSR()){
//                                pVoiceSR = errorSeg.getpVoiceSRMsg();
                                pVoiceSR = "9";
                            }

                            if(errorSeg.getAmbVerbCond()){

//                                ambVerbCond = errorSeg.getAmbVerbCondMsg();
                                ambVerbCond = "10";
                            }
                            if(errorSeg.getAmbVerbSR()){
//                                ambVerbSR = errorSeg.getAmbVerbSRMsg();
                                ambVerbSR = "10";
                            }

                            if(counterClausesInternal==1)
                            {
                                reqLine.add(nonAtomic);//[3] 4 NonAtomic
                                reqLine.add(incompReq);//[4] 5 IncompleteReq
                                reqLine.add(unOrdReq);//[5] 6 UnorderedReq
                                reqLine.add(coordAmb);//[6] 7 CoordAmb
                                reqLine.add(notReq);//[7]8 NotReq
                            }else
                            {
                                reqLine.add("");//[3]4 NonAtomic
                                reqLine.add("");//[4]5 IncompleteReq
                                reqLine.add("");//[5]6 UnorderedReq
                                reqLine.add("");//[6] 7 CoordAmb
                                reqLine.add("");//[7] 8 NotReq
                            }




                            reqLine.add(noActorCond); //6 [8]
                            reqLine.add(noVerbCond); // 6 [9]
                            reqLine.add(noActorSR); //7 [10]
                            reqLine.add(noModalSR); //7 [11]
                            reqLine.add(noVerbSR); //7 [12]
                            reqLine.add(""); //8 [13]
                            reqLine.add(pVoiceCond);//9 [14]
                            reqLine.add(pVoiceSR);//9 [15]
                            reqLine.add(ambVerbCond);//10 [16]
                            reqLine.add(ambVerbSR );//10 [17]


                        }

                        csvWriter.writeNext(reqLine.toArray(new String[0]));
                    }
                }else
                {
                    clauseCounter +=1;
                    ArrayList<String> reqLine = new ArrayList<String>();
                    reqLine.add(req.getReqId());   //#1
                    reqLine.add(req.getReqId()+"_"+clauseCounter.toString());//#2
                    reqLine.add(req.getPreprocessedReq());  //[2] #3
                    if(recomPattern )
                    {
                        reqLine.add(" ");   //  #4
                        reqLine.add(" ");   //  #5
                        reqLine.add(" ");   //  #6
                        reqLine.add(" ");   //  #[6]7

                    }

                    if( smells)
                    {
                        reqLine.add(" ");   //  #[3]4
                        reqLine.add(" ");   //  #5
                        reqLine.add(" ");   //  #6
                        reqLine.add(" ");   //  #7
                        reqLine.add(" ");   //  #8
                        reqLine.add(" ");   //  #9
                        reqLine.add(" ");   //  #10
                        reqLine.add(" ");   //  #11
                        reqLine.add(" ");   //  #12
                        reqLine.add(" ");   //  #13
                        reqLine.add(" ");   //  #14
                        reqLine.add(" ");   //  #15
                        reqLine.add(" ");   //  #16
                        reqLine.add(" ");   //  #17


                    }
                    csvWriter.writeNext(reqLine.toArray(new String[0]));
                }
            }
            csvWriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return wordsReq;

    }


}
