import edu.stanford.nlp.ling.TaggedWord;

import java.util.ArrayList;
import java.util.List;

public class SegmentReq {
    private String segmentReq;
    private String originalReq;

    private int startSegment;
    private int endSegment;
    private boolean isMatched;
    private String typeOfClause;
    private List<TupleConcept> conceptsInText;
    private String actorCondition;

    private String verbCondition;
    private String pennTreeBankVerbCondition;
    private ArrayList<TaggedWord> condPosTags;

    private String srActor;
    private String srModal;

    private String srVerb;
    private String pennTreeBankVerbSR;
    private ArrayList<TaggedWord> verbPosTags;

    private String verbCondInc; //Incomplete

    private String passiveVoiceCond;
    private String passiveVoiceSR;
    private SmellSegment errorsSegment;

    //public SegmentReq(String req, String segmentReq, int startSegment, int endSegment)
    public SegmentReq(int startSegment, int endSegment, boolean isMatched, String typeClause, String passiveText, String passiveTextSR )
    {

        this.startSegment = startSegment;
        this.endSegment = endSegment;
        this.isMatched = isMatched;
        this.typeOfClause = typeClause;
        this.conceptsInText = new ArrayList<>();
        this.passiveVoiceCond = passiveText;
        this.passiveVoiceSR = passiveTextSR;

    }


    public int getEndSegment() {
        return endSegment;
    }

    public int getStartSegment()
    {
        return startSegment;
    }

    public String getPassiveVoiceCond(){return this.passiveVoiceCond; }
    public String getPassiveVoiceSR(){return this.passiveVoiceSR; }

    public void setStartSegment(int start)
    {
        this.startSegment = start;
    }

    public void setEndSegment(int end)
    {
        this.endSegment = end;
    }

    public void setPassiveVoiceCond(String passiveText) {this.passiveVoiceCond = passiveText; }
    public void setPassiveVoiceSR(String passiveTextSR) {this.passiveVoiceSR = passiveTextSR; }

    public boolean isMatched() {
        return isMatched;
    }
    public void setIsMatched(boolean match)
    {
        this.isMatched = match;
    }

    public void setTypeOfClause(String type)
    {
        this.typeOfClause = type;
    }

    public String getTypeOfClause() {return this.typeOfClause;}

    public String getSegmentReq()
    {
        return  this.segmentReq;
    }

    public void setCondPosTags(ArrayList<TaggedWord> condTags){ this.condPosTags = condTags; }
    public ArrayList<TaggedWord> getCondPosTags() { return  this.condPosTags; }

    public void setVerbPosTags(ArrayList<TaggedWord> verbTags){ this.verbPosTags = verbTags; }
    public ArrayList<TaggedWord> getVerbPosTags() { return this.verbPosTags; }

    public String getOriginalReq() { return this.originalReq; }

    public List<TupleConcept> getConceptsInText(){ return this.conceptsInText; }

    public void setConceptsInText(List<TupleConcept> concepts)
    {
        this.conceptsInText = concepts;
    }

    public void mergeTypeOfClause(String type)
    {

        if(!this.typeOfClause.equals(type))
        {
            this.typeOfClause+= " "+type;
        }
    }

    public String mergeTypeOfClause2(String type)
    {
        if(!this.typeOfClause.equals(type))
        {
            this.typeOfClause+= " "+type;
        }
        return this.typeOfClause;
    }

    public String mergePassiveVCond(String pvCondB)
    {
        String pasVoicCond = "";
        if(this.getPassiveVoiceCond().trim() == pvCondB.trim())
        {
            pasVoicCond = this.getPassiveVoiceCond();
        }else
        {
            pasVoicCond = this.getPassiveVoiceCond()+" "+pvCondB;
        }
        return pasVoicCond;
    }

    public String mergePassiveVSR(String pvSrB)
    {
        String pasVoicSR = "";
        if(this.getPassiveVoiceSR() == pvSrB)
        {
            pasVoicSR = this.getPassiveVoiceSR();
        }else
        {
            pasVoicSR = this.getPassiveVoiceSR()+" "+pvSrB;
        }
        return pasVoicSR;
    }
    public String mergeVerbCond(String condB){
        String verbCond = "";
        if(this.getVerbCondition() == condB){
            verbCond = this.getVerbCondition();
        }else{
            verbCond = this.getVerbCondition()+" "+condB;
        }
        return verbCond;
    }


    public void mergeConcepts(List<TupleConcept> conceptsExt)
    {
        for(TupleConcept c: conceptsExt)
        {
            if(!containConcept(c))
            {
                this.conceptsInText.add(c);
            }
        }
        List<TupleConcept> conceptsInText;
    }

    public List<TupleConcept> mergeConcepts2(List<TupleConcept> conceptsExt)
    {
        for(TupleConcept c: conceptsExt)
        {
            if(!containConcept(c))
            {
                this.conceptsInText.add(c);
            }
        }
        return this.conceptsInText;
    }

    public Boolean containConcept(TupleConcept concept)
    {
        boolean hasConcept = false;
        if(this.conceptsInText.size() > 0)
        {
            for(TupleConcept c: this.conceptsInText)
            {
                if(c.getContentConcept().equals(concept.getContentConcept()) && c.getNameConcept().equals(concept.getNameConcept()))
                {
                    hasConcept = true;
                }
            }
        }
        return  hasConcept;
    }

    public void mapConcepts(List<TupleConcept> conceptsInText)
    {
        this.setActorCondition(null);
        this.setVerbCondition(null);
        this.setPennTreeBankVerbCondition(null);
        this.setSrActor(null);
        this.setSrModal(null);
        this.setSrVerb(null);
        this.setPennTreeBankVerbSR(null);
        this.setVerbCondInc(null);

        for (TupleConcept concept: conceptsInText)
        {

            switch(concept.getNameConcept()) {
                case "actorCondition":
                    if(IdentifySegmentReqs.hasClauseConcepts(conceptsInText,"actorCondition"))
                    {
//                        System.out.println("ConceptAA0: "+concept.getContentConcept());
                        this.setActorCondition(concept.getContentConcept());// code block
//                        System.out.println("ConceptAA1: "+this.getActorCondition());
                    }
                    break;
                case "verbCondition":
                    if(IdentifySegmentReqs.hasClauseConcepts(conceptsInText,"verbCondition"))
                    {
                        this.setVerbCondition(concept.getContentConcept());// code block
                        this.setPennTreeBankVerbCondition(concept.getPennTreeConcept());
                        this.setCondPosTags(concept.getPosConcept());

                    }
                    break;
                case "actor":
                    if(IdentifySegmentReqs.hasClauseConcepts(conceptsInText,"actor"))
                    {
                        this.setSrActor(concept.getContentConcept());
                    }
                    break;
                case "modal":
                    if(IdentifySegmentReqs.hasClauseConcepts(conceptsInText,"modal"))
                    {
                        this.setSrModal(concept.getContentConcept());
                    }
                    break;
                case "verb":
                    if(IdentifySegmentReqs.hasClauseConcepts(conceptsInText,"verb"))
                    {
                        this.setSrVerb(concept.getContentConcept());
                        this.setPennTreeBankVerbSR(concept.getPennTreeConcept());
                        this.setVerbPosTags(concept.getPosConcept());
                    }
                    break;
                case "verbCondInc":
                    if(IdentifySegmentReqs.hasClauseConcepts(conceptsInText,"verbCondInc"))
                    {
                        this.setVerbCondInc(concept.getContentConcept());

                    }
                    break;

            }
        }
    }


    public String getActorCondition()
    {
        return this.actorCondition;
    }

    public void setActorCondition(String actorCondition) {
        this.actorCondition = actorCondition;
    }

    public String getVerbCondition()
    {
        return this.verbCondition;
    }

    public void setVerbCondition(String verbCondition) {
        this.verbCondition = verbCondition;
    }

    public String getPennTreeBankVerbCondition()
    {
        return this.pennTreeBankVerbCondition;
    }
    public void setPennTreeBankVerbCondition(String pennTreeBankVerbCondition)
    {
        this.pennTreeBankVerbCondition = pennTreeBankVerbCondition;
    }

    public String getSrActor()
    {
        return this.srActor;
    }

    public void setSrActor(String srActor) {
        this.srActor = srActor;
    }

    public String getSrModal()
    {
        return this.srModal;
    }

    public void setSrModal(String srModal) {
        this.srModal = srModal;
    }

    public String getSrVerb()
    {
        return this.srVerb;
    }

    public void setSrVerb(String srVerb) {
        this.srVerb = srVerb;
    }

    public String getPennTreeBankVerbSR()
    {
        return this.pennTreeBankVerbSR;
    }

    public void setPennTreeBankVerbSR(String pennTreeBankVerbSR) {
        this.pennTreeBankVerbSR = pennTreeBankVerbSR;
    }

    public void setVerbCondInc(String verbCondInc) {
        this.verbCondInc = verbCondInc;
    }

    public String getVerbCondInc() {
        return verbCondInc;
    }


    public void setErrorsSegment (SmellSegment smellsSeg) {
        this.errorsSegment =  smellsSeg;
    }

    public SmellSegment getErrorsSegment () {
        return this.errorsSegment;
    }



}
