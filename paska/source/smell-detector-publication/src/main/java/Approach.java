
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.*;
import java.util.*;
import java.util.HashMap;

/*
This class contain the main method of the smell detector
* */

public class Approach {

    /*
    * This method analyze requirement per requirement the following:
    * - identifies the segments/clauses of the requirement
    * - identifies the smells found in the requirements or in the segments/clauses
    * */
    public static List<Requirement> identifySegmenFindSmells(String extension, File sra, File folderInput, HashMap<String, String> patternsPartsReq, MaxentTagger tagger){

        List<Requirement> requirements = new ArrayList<Requirement>();
        //Check SRA by SRA
        if (sra.isFile() && extension.equals("csv"))
        {
            System.out.println("File " + sra.getName());
            String nameFile = sra.getName();
            ArrayList<ArrayList<String>> parsingTrees = Utils.getParsingTreesFromCsv(folderInput + "/" + nameFile);
            //To extract the concepts of the requirements
            List<String> conceptToExtract = new ArrayList<>();
            conceptToExtract.add("actorCondition");
            conceptToExtract.add("verbCondition");
            //To extract concepts from system response
            conceptToExtract.add("actor");
            conceptToExtract.add("modal");
            conceptToExtract.add("verb");


            //Counters for smells
            int NonAtomic =0;
            int IncompReq =0;
            int UnOrderReq =0;
            int coordAmb =0;
            int notReq =0;
            int noActorCond=0;
            int noVerbCond=0;
            int noModalSR=0;
            int noVerbSR=0;
            int noNounScope =0;
            int pVCond =0;
            int pVSR =0;
            int ambVerbCond =0;
            int ambVerbSR =0;


            // Review requirement by requirement
            for(ArrayList<String> tree:parsingTrees)
            {
                ArrayList<MatchPattern> uniqClausesInReq = new ArrayList<>();
                List<SegmentReq> orderedRangesReq = new ArrayList();
                //Extracting from the csv line the reqid, parsing tree, etc
                String reqID = tree.get(0);
                String reqParsingTree = tree.get(1);
                String originalReq = tree.get(2);
                String preprocessedReq = tree.get(3);

                String patternMatched = "";
                ArrayList<String> valuesColumn = new ArrayList<String>();
                SmellReq errorsReq = new SmellReq(reqID);
                int counter = 0;

                //Apply the Tregex patterns to a requirement
                for(Map.Entry<String, String> entry : patternsPartsReq.entrySet()) {
                    counter++;
                    if(Utils.patternMatchInReq(reqParsingTree, entry.getValue()))
                    {
                        //identinfy segments/clauses from requirements
                        ArrayList<MatchPattern> textInReq = IdentifySegmentReqs.extractClausesFromReq(reqParsingTree, entry.getValue(),entry.getKey(), tagger, reqID);
                        uniqClausesInReq.addAll(textInReq);
                    }
                }

                if(uniqClausesInReq.size()>0)
                {
                    //Methods to remove some segments/clauses that contain matched text to avoid having duplicates
                    ArrayList<MatchPattern> uniqClausesInReqAfterScope = IdentifySegmentReqs.removeScopeFromSR(uniqClausesInReq);
                    ArrayList<MatchPattern> uniqClausesInReqAfterCond  = IdentifySegmentReqs.removeCondAfterSR(uniqClausesInReqAfterScope);
                    ArrayList<MatchPattern> uniqClausesInReqAfterCondScope = IdentifySegmentReqs.removeCondAfterScope(uniqClausesInReqAfterCond);
                    List<SegmentReq> clausesInReq =  Utils.getIndexesClausesInReq(uniqClausesInReqAfterCondScope, preprocessedReq, tagger, reqID);

                    if(clausesInReq.size() > 0)
                    {
                        //Methods to merge clauses/segments in requirements
                        List<SegmentReq> clausesMerged = Utils.mergeOverlapClausesNew(clausesInReq);
                        List<SegmentReq> clausesNoDupl = Utils.removeDuplicatesClausesInReq(clausesMerged,tagger,reqID, preprocessedReq);
                        //Methods to identify the different segments on requirements
                        List<SegmentReq> clausesAfterMultCondSr = Utils.matchMultCondSr(clausesNoDupl,tagger, preprocessedReq, reqID);
                        List<SegmentReq> clausesAfterPosAnalysis = PosMethods.getTypesInsideSegments(clausesAfterMultCondSr, tagger, preprocessedReq, reqID);
                        List<SegmentReq> notMatchSegmentsInReq = Utils.getNotMatchSegmentsInReq(clausesAfterMultCondSr,preprocessedReq);
                        List<SegmentReq> segmentAftPosAnalysis = PosMethods.labelNotMatchSegs(notMatchSegmentsInReq,preprocessedReq, tagger, reqID);
                        orderedRangesReq = Utils.orderRanges(clausesAfterMultCondSr,segmentAftPosAnalysis);
                        orderedRangesReq = PosMethods.notMatchedNearestComp(orderedRangesReq,preprocessedReq, tagger, reqID);

                        //define type of requirement
                        patternMatched = PatternRecom.defineTypeReq(orderedRangesReq, preprocessedReq, reqID);
                        //identify smells segment and requirement levels
                        orderedRangesReq = Utils.identifySmellsSegsLevel(orderedRangesReq, reqID,preprocessedReq, tagger);
                        errorsReq = FindSmell.identifySmellsReqLevel(orderedRangesReq, reqID,preprocessedReq, tagger, patternMatched );

                    }else
                    {
                        System.out.println("There was an error");
                    }

                }else
                {
                    //Tregex patterns did not work then we apply we analyze the requirement using POS methods
                    List<MatchPattern> segmentsReq = PosMethods.getTypesSegmentAftAnalysis(preprocessedReq,tagger);
                    List<SegmentReq> clausesInReq =  Utils.getIndexesClausesInReq(segmentsReq, preprocessedReq, tagger, reqID);

                    if(clausesInReq.size()>=1)
                    {
                        orderedRangesReq = new ArrayList<>(clausesInReq);
                        //we identify the type of the requirement
                        patternMatched = PatternRecom.defineTypeReq(orderedRangesReq, preprocessedReq, reqID);
                        //we identify smells
                        orderedRangesReq = Utils.identifySmellsSegsLevel(clausesInReq, reqID, preprocessedReq, tagger);
                        errorsReq = FindSmell.identifySmellsReqLevel(clausesInReq, reqID,preprocessedReq, tagger,patternMatched);

                    }else
                    {
                        orderedRangesReq = null;
                        patternMatched = "otherType";
                    }
                }

                //We create a requirement including requirement id, original req,  all clauses/segements we identified, pattern we matched,
                // and the preprocessed requirement
                Requirement singleReq = new Requirement(reqID, originalReq, orderedRangesReq, patternMatched, preprocessedReq);

                //adding to the previously created requirement the smells we identified
                singleReq.setSmellsReq(errorsReq);

                if(errorsReq.getNonAtomic()){
                    NonAtomic ++;
                }
                if(errorsReq.getIncompReq()){
                    IncompReq ++;
                }
                if(errorsReq.getUnorderCondReq() || errorsReq.getUnorderScopeReq()){
                    UnOrderReq ++;
                }
                if(errorsReq.getCoordAmbCommaOr()){
                    coordAmb ++;
                }
                if(errorsReq.getNotRequirement()){
                    notReq ++;
                }

                //Counters of the Smells
                if(singleReq.getOrderSegs()!=null ){
                    for(SegmentReq s : singleReq.getOrderSegs())
                    {
                        SmellSegment smell = s.getErrorsSegment();

                        if(smell.getNoActorCond()){
                            noActorCond++;
                        }
                        if(smell.getNoVerbCond()){
                            noVerbCond++;
                        }
                        if(smell.getNoModalSR()){
                            noModalSR++;
                        }
                        if(smell.getNoVerbSR()){
                            noVerbSR++;
                        }
                        if(smell.getNoNounScope()){
                            noNounScope++;
                        }
                        if(smell.getPVoiceCond()){
                            pVCond ++;
                        }
                        if(smell.getPVoiceSR()){
                            pVSR ++;
                        }
                        if(smell.getAmbVerbCond()){
                            ambVerbCond ++;
                        }
                        if(smell.getAmbVerbSR()){
                            ambVerbSR ++;
                        }
                    }
                }
                requirements.add(singleReq);
            }


            System.out.println("Smells found in this document: ");
            System.out.println("Non-atomic requirement: "+NonAtomic+", Incomplete requirement: "+IncompReq+", Incorrect order requirement: "+UnOrderReq+", Coordination ambiguity: " + coordAmb +", Not requirement: ," +notReq);
            System.out.println("Incomplete condition: "+ (noActorCond+noVerbCond)+", Incomplete system response: "+  (noModalSR+noVerbSR)+", "+ "Passive voice: "+ (pVCond+pVSR)+", "+"Not precise verb: "+ (ambVerbCond+ ambVerbSR));



        }else if (sra.isDirectory()) {
            System.out.println("Directory " + sra.getName());
        }
        return requirements;
    }


    public static void main (String args[])
    {
       // if (args.length == 3){ // For using with jar files

            String pathCsvDocuments = ""; //Specify the path to the folder that contains the parsing trees 
            String csvFolderOut = ""; //Specify the path to the folder that will store the file(s) with detected smells 
            String taggerModelPath = ""; //Specify the path to stanford tagger  english-left3words-distsim.tagger you should donwload from the standford web site

            // This is when you want to run using JAr File
            /*String pathCsvDocuments = args[0];
            String csvFolderOut = args[1];
            String taggerModelPath = args[2];*/

            File folderInput    = new File(pathCsvDocuments);
            File folderOut      = new File(csvFolderOut);
            File[] listOfFiles = folderInput.listFiles();
            ArrayList<Integer> numberWordsDataSet = new ArrayList<Integer>();
            MaxentTagger tagger =  new MaxentTagger(taggerModelPath);

            for (int i = 0; i < listOfFiles.length; i++)
            {
                String fileName = listOfFiles[i].getName();
                String extension = Utils.getExtension(fileName).trim();

                //Patterns for identifying segments/clauses from requirements
                HashMap<String, String> patternsPartsReq = ExtractionPattern.loadPatterns();
                //List of requirements that will be split and will contain the smells at requirement and segment levels
                List<Requirement> requirements = identifySegmenFindSmells(extension,listOfFiles[i],folderInput,patternsPartsReq, tagger);
                System.out.println("Number Req with smells: "+Utils.numberReqWithSmells(requirements));

                //Create the Csv that will contain the clauses along with the requirements smells
                
                //For user friendly output
                //ArrayList<Integer> wordsInSra = Output.createCsvClauses(requirements, folderOut+"/"+listOfFiles[i].getName(),true, true,  tagger);

                /* For generating output for experiments
                * In the function parameters just set either :
                *   - (requirements, folderOut+"/"+listOfFiles[i].getName(),true, false,  tagger) // Smell  Detector
                *   - (requirements, folderOut+"/"+listOfFiles[i].getName(),false, true,  tagger) // Pattern Recommendation
                * */

                ArrayList<Integer> wordsInSra = Output.createCsvClausesExperiment(requirements, folderOut+"/"+listOfFiles[i].getName(),true, false,  tagger);

                numberWordsDataSet.addAll(wordsInSra);
            }
            Utils.printFrequencies(numberWordsDataSet);


//        }else{
//            System.out.println("Please provide the paths for the  input and the output folders.");
//        }

    }

}
