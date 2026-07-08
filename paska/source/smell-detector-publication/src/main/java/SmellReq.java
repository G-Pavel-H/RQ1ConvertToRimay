public class SmellReq {

    private final String nonAtomicMsg = "The requirement has more than one action in the system response.";
    private final String incompReqMsg = "The requirement does not have a system response.";
    private final String unorderCondReqMsg = "The condition clause of the requirement is after the system response.";
    private final String unorderScopeReqMsg = "The scope clause of the requirement is not on its incorrect position.";
    private final String coordAmbOrMsg = "Requirement contains multiple conditions separated by conjunction OR.";
    private final String coordAmbCommaMsg =  "Requirement contains multiple conditions separated by comma.";
    private final String notReqMsg = "The sentence does not contain a system response nor condition nor a scope phrase.";
    private String reqID;
    private boolean nonAtomic;
    private boolean incompReq;
    private boolean unorderCondReq;
    private boolean unorderScopeReq;
    private boolean coordAmbCommaOr;
//    private boolean coordAmbComma;
    private boolean notRequirement;



    public SmellReq(String reqID)
    {
        this.reqID = reqID;

    }
    public String getNonAtomicMsg() {
        return nonAtomicMsg;
    }
    public String getIncompReqMsg() {
        return incompReqMsg;
    }
    public String getUnorderCondReqMsg() {
        return unorderCondReqMsg;
    }
    public String getUnorderScopeReqMsg() {
        return unorderScopeReqMsg;
    }
    public String getCoordAmbCommaOrMsg() {
        return coordAmbOrMsg;
    }
    public String getCoordAmbCommaMsg() {
        return coordAmbCommaMsg;
    }
    public String getNotReqMsg() {
        return notReqMsg;
    }
    public String getReqID() {
        return reqID;
    }


    public void setNonAtomic(boolean nonAtomic) {
        this.nonAtomic = nonAtomic;
    }

    public void setIncompReq(boolean incompReq) {
        this.incompReq = incompReq;
    }

    public void setUnorderCondReq(boolean unorderCondReq) {
        this.unorderCondReq = unorderCondReq;
    }

    public void setUnorderScopeReq(boolean unorderScopeReq) {
        this.unorderScopeReq = unorderScopeReq;
    }

    public void setCoordAmbCommaOr(boolean coordAmbCommaOr) {
        this.coordAmbCommaOr = coordAmbCommaOr;
    }



    public void setNotRequirement(boolean notRequirement) {
        this.notRequirement = notRequirement;
    }


    public boolean getNonAtomic() {
        return this.nonAtomic;
    }

    public boolean getIncompReq() {
        return this.incompReq;
    }
    public boolean getUnorderCondReq() {
        return this.unorderCondReq;
    }
    public boolean getUnorderScopeReq() {
        return this.unorderScopeReq;
    }
    public boolean getCoordAmbCommaOr() {
        return this.coordAmbCommaOr;
    }

    public boolean getNotRequirement() {
        return notRequirement;
    }
}
