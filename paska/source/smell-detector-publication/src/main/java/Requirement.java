import java.util.ArrayList;
import java.util.List;

public class Requirement
{
    public String reqId;
    public String originalReq;
    public List<SegmentReq> orderSegs;
    public String patternMatched;
    public String preprocessedReq;
    public SmellReq smellsReq;


    public Requirement(String reqId, String originalReq,  List<SegmentReq> orderSegments, String pattern, String preprocessedReq)
    {
        this.reqId = reqId;
        this.originalReq = originalReq;
        this.orderSegs = orderSegments;
        this.patternMatched = pattern;
        this.preprocessedReq = preprocessedReq;
    }
    public void setReqId(String reqId) {
        this.reqId = reqId;
    }
    public void setOriginalReq(String originalReq) {
        this.originalReq = originalReq;
    }
    public void setOrderSegs(List<SegmentReq> segmentsInReq)
    {
        this.orderSegs = segmentsInReq;
    }
    public String getReqId()
    {
        return this.reqId;
    }

    public String getOriginalReq()
    {
        return this.originalReq;
    }
    public List<SegmentReq> getOrderSegs()
    {
        return this.orderSegs;
    }

    public void setPatternMatched(String pattern)
    {
        this.patternMatched = pattern;
    }
    public String getPatternMatched(){return  this.patternMatched;}

    public void setPreprocessedReq(String preprocess)
    {
        this.preprocessedReq = preprocess;
    }
    public String getPreprocessedReq(){return  this.preprocessedReq;}

    public void setSmellsReq(SmellReq smellsReq) {
        this.smellsReq = smellsReq;
    }

    public SmellReq getSmellsReq() {
        return smellsReq;
    }
}
