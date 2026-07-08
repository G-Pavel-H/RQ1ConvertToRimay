import java.util.List;

/*
* Method that returns the type of requirement. This output will be used to recommend a Rimay pattern
*  */

public class PatternRecom {

    public static String defineTypeReq(List<SegmentReq> segments, String preprocessedReq, String reqID)
    {
        int countCondBefore = 0;
        int countCondAfter = 0;

        int countScope = 0;
        int countScopeAft = 0;

        int countSR = 0;
        int countSRStart = 0;

        String typeRequirement = "";
        //We assume that all the components are ordered
        countCondBefore = FindSmell.numCondBefore(segments);
        countCondAfter = FindSmell.numCondAfter(segments);

        countScopeAft = FindSmell.numScopeAft(segments);
        countScope = FindSmell.numScopeBef(segments, reqID);

        countSR = FindSmell.numSREnd(segments);
        countSRStart = FindSmell.numSRStart(segments);


        if( ((countScopeAft + countScope) >= 1) && (countCondAfter + countCondBefore) > 1 && ((countSR+countSRStart)==1) ){
            typeRequirement = "scopeMultCondSysResp";
        }else if(((countScopeAft + countScope) == 0) && (countCondAfter + countCondBefore) > 1 && ((countSR+countSRStart)==1)){
            typeRequirement = "multCondSysResp";
        }else if( ((countScopeAft + countScope) >= 1) && (countCondAfter + countCondBefore) > 1 && ((countSR+countSRStart)>1) ){
            typeRequirement = "scopeMultCondMultSrs";
        }else if(((countScopeAft + countScope) == 0) && (countCondAfter + countCondBefore) > 1 && ((countSR+countSRStart)>1)){
            typeRequirement = "multCondMultSrs";
        }else if(((countScopeAft + countScope) == 0) && (countCondAfter + countCondBefore) == 1 && ((countSR+countSRStart)==1)){
            typeRequirement = "condSysResp";
        }else if(((countScopeAft + countScope) == 0) && (countCondAfter + countCondBefore) == 1 && ((countSR+countSRStart)>1)){
            typeRequirement = "condMultSrs";
        }else if(((countScopeAft + countScope) == 0) && (countCondAfter + countCondBefore) == 0 && ((countSR+countSRStart)==1)){
            typeRequirement = "sysResp";
        }else if(((countScopeAft + countScope) == 0) && (countCondAfter + countCondBefore) == 0 && ((countSR+countSRStart)>1)){
            typeRequirement = "multSrs";
        }else if(((countScopeAft + countScope) >= 1) && (countCondAfter + countCondBefore) ==1 && ((countSR+countSRStart)==1) ){
            typeRequirement = "scopeCondSysResp";
        }else if(((countScopeAft + countScope) >= 1) && (countCondAfter + countCondBefore) ==1 && ((countSR+countSRStart)>1) ){
            typeRequirement = "scopeCondMultSrs";
        }else if(((countScopeAft + countScope) >= 1) && (countCondAfter + countCondBefore) ==0 && ((countSR+countSRStart)==1) ){
            typeRequirement = "scopeSysResp";
        }else if(((countScopeAft + countScope) >= 1) && (countCondAfter + countCondBefore) ==0 && ((countSR+countSRStart)>1) ){
            typeRequirement = "scopeMultSrs";
        }else{
            typeRequirement = "otherType";
        }
        return  typeRequirement;
    }
}
