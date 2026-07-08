import java.util.HashMap;

public class ExtractionPattern {
    /*
    * Tregex patterns to identify segments (conditions, scope, and system response) in the requirements
    * */

    public static HashMap<String,String> loadPatterns()
    {
        HashMap<String, String> patternsExtraction = new HashMap<String, String>();

        patternsExtraction.put("conditionS1", "(/WH.?/=conjunction $+ (S <, (S=clause < (/N.?/=actorCondition $+ (VP=verbPhraseCond <<, /VB.?/=verbCondition))))) > SBAR &!>> VP");
        patternsExtraction.put("conditionS3", "((IN=conjunction < /^(Once|once)$/)  $+ (S=clause < (/N.?/=actorCondition $+ (VP=verbPhraseCond <<, /VB.?/=verbCondition)))) [> SBAR | > S] & > !VP");
        //After SR
        patternsExtraction.put("conditionS4", "((SBAR < ((S < (/N.?/=actorCondition $+(VP=verbPhraseCond <<, /VB.?/=verbCondition))) [$- (IN !< /^(that|with)$/)  |  $- WHADVP | $- WRB ])) $-- NP) >> (((VP < (MD ?$+ ADVP)) $- (NP [< /NN.?/|< NP|< PRP])) >> S)");
        //PatternCondition S5 - S7 Complete condition but not under common sub-trees
        patternsExtraction.put("conditionS5", "((/(IN|WHADVP|WRB)/=conjunction !< /(of|to|in|In|at|At|on|On)/)  $+ (S=clause < (NP=actorCondition $+ (VP=verbPhraseCond <<, (/VB.?/=verbCondition $++ NP | $++ PP)))) !>> /(VP|SBAR)/)");  // 17 Oct After the changes
        patternsExtraction.put("conditionS6", "((/(IN|WHADVP|WRB)/=conjunction !< /(of|to|in|In|at|At|on|On)/)  $+ (NP=nounPhraseCond $+ (VP=verbPhraseCond <<, (/VB.?/=verbCondition $++  NP | $++ PP ) )) !>> /(VP|SBAR)/)"); // 17 Oct After the changes


        //Common
        patternsExtraction.put("conditionS7", "(SBAR < ((/^(IN|WHADVP|WRB)$/ !<< that) $+ (S << (SBAR <<, /(That|that)/) &<<,  (/N.?/=actorCondition $++ (VP=verbPhraseCond [ <<, /VB.?/=verbCondition | < (VP <<, /VB.?/=verbCondition)]) | $++ /VB.?/=verbCondition)))) !>> /(VP)/");
        patternsExtraction.put("conditionS8", "(SBAR < ((/^(IN|WHADVP|WRB)$/ !<< (that|as)) $+ (S !<< SBAR &<<,  (/N.?/=actorCondition $++ (VP=verbPhraseCond [ <<, /VB.?/=verbCondition | < (VP <<, /VB.?/=verbCondition)]) | $++ /VB.?/=verbCondition)))) !>> /(VP)/");

        //Condition Incomplete
        patternsExtraction.put("conditionS9","((SBAR < (/(IN|WHADVP|WRB)/ $+ (S < ((VP < (VBG=verbCondInc $+ NP | $+ PP)) !$++ NP !$--NP )))) !>> VP )");
        patternsExtraction.put("conditionS10","((PP < ( (IN < (/^(Upon|upon|On|on)$/)) $+ (NP < ( (NP << NN=verbCondInc) $++ PP )))) !>> /(VP|SBAR)/)");
        patternsExtraction.put("conditionS11","(((IN=conjunction < (/^(Upon|upon|On|on|After|after|Before|before)$/)) $+ (NP=nounPhraseCond < ((NP << NN=verbCondInc)  $++ PP ))) !>> /(VP|SBAR)/) !>> PP "); // 18 Odt after the changes


        //Time condition
        patternsExtraction.put("conditionS12","(PP < (IN<(/^(After|after|Before|before)$/) $+ (NP !< /VB.?/ < /N.?/) )) > S "); //18Oct After the changes


        //Scope
        patternsExtraction.put("scopeSC1","((PP < ((IN < /^(For|for)$/) $+ (NP [<<, /N.?/ | <<, DT]))) > (S < (/(RB|,|ADVP)/ $+ (NP $+ VP) | $+ (S < (NP $+ VP)))))");
        patternsExtraction.put("scopeSC2","((PP < ((IN < /^(For|for)$/) $+ (NP [<<, /N.?/ | <<, DT]))) [>- ((VP < MD) $- NP) | >- (VP >-((VP< MD) $- NP))] & !>> PP)");

        //SystemResponse
        patternsExtraction.put("sysrespSR","((NP=nounPhrase < /\\b(NN.?|NP|PRP)\\b/=actor) $+ (VP=verbPhrase  < ( MD=modal ?$+ ADVP $++  (VP=VerbPhraseSR <<, (VB=verb $++ NP | $++ PP) | <<, (VP < (/VB.?/=verb $++ NP | $++ PP)) | <<, (/VB.?/=verb  $+ (S < (NP $++ VP)) ) | <<, (/VB.?/=verb  $+ (S < (VP < (TO $+ VP) ))) | <<, (/VB.?/=verb  $++ (VP < /VB?/)) | <<, (/VB.?/=verb $+ (SBAR < ((IN < that) $+ S)))| <<, (/VB.?/   ($+( ADJP < (JJ $+ (S< (VP < ( TO $+ VP | $+ /VB.?/=verb) )))))) | <<, (/VB.?/  $+ (ADJP < JJ)  $++ (S < (VP < (TO $+ VP=verb)))) | <<, ( /VB.?/ ($+ JJ $++ S)))     ))) > S & !>> /^(SBAR)$/ &$-- /[A-Za-z]/");
        patternsExtraction.put("sysrespStartSR","((NP=nounPhrase < /\\b(NN.?|NP|PRP)\\b/=actor) $+ (VP=verbPhrase  < ( MD=modal ?$+ ADVP $++ (VP=VerbPhraseSR <<, (/VB.?/=verb $++ NP | $++ PP) | <<, (VP < (/VB.?/=verb $++ NP | $++ PP)) | <<, (/VB.?/=verb  $+ (S < (NP $++ VP)) ) | <<, (/VB.?/=verb  $+ (S < (VP < (TO $+ VP) ))) | <<, (/VB.?/=verb  $++ (VP < /VB?/)) | <<, (/VB.?/=verb $+ (SBAR < ((IN < that) $+ S)))| <<, (/VB.?/   ($+( ADJP < (JJ $+ (S< (VP < ( TO $+ VP | $+ /VB.?/=verb) )))))) | <<, (/VB.?/  $+ (ADJP < JJ)  $++ (S < (VP < (TO $+ VP=verb)))) | <<, ( /VB.?/  ($+ JJ $++ S) ))    ))) >, S & !>> /^(SBAR|VP|NP)$/");

        return patternsExtraction;
    }

    //Tregex Patterns to identify passive voice in the SR
    public static HashMap<String, String> loadPatternsPassiveVoiceSR()
    {
        HashMap<String, String> patternsExtractionSR = new HashMap<String, String>();

        patternsExtractionSR.put("sysrespPassiveVoice", "((NP=nounPhrase < /\\b(NN.?|NP|PRP)\\b/) $+ (VP=verbPhrase  < ( MD ?$+ ADVP $++ (VP < ( ((/VB.?/ < be) $+ (VP <, VBN)) $++ NP | $++ PP) | < (VP < (((/VB.?/ < be) $+ (VP <, VBN)) $++ NP | $++ PP)) | < (((/VB.?/ < be) $+ (VP <, VBN))  $+ (S < (NP $++ VP)) ) | < (((/VB.?/ < be) $+ (VP <, VBN))  $+ (S < (VP < (TO $+ VP) ))) | < (((/VB.?/ < be) $+ (VP <, VBN))  $++ (VP < /VB?/)) | < (((/VB.?/ < be) $+ (VP <, VBN)) $+ (SBAR < ((IN < that) $+ S)))| < (((/VB.?/ < be) $+ (VP <, VBN))   ($+( ADJP < (JJ $+ (S< (VP < ( TO $+ VP | $+ /VB.?/) )))))) | < (((/VB.?/ < be) $+ (VP <, VBN))  $+ (ADJP < JJ)  $++ (S < (VP < (TO $+ VP)))) | < ( ((/VB.?/ < be) $+ (VP <, VBN))  ($+ JJ $++ S) )) ))  ) > S & !>> /^(SBAR)$/ &$-- /[A-Za-z]/");
        patternsExtractionSR.put("sysrespPassiveVoice2","((NP=nounPhrase < /\\b(NN.?|NP|PRP)\\b/) $+ (VP=verbPhrase  < ( MD ?$+ ADVP $++ (VP < (((/VB.?/ < be) $+ (VP <, VBN)) $++ NP | $++ PP) | < (VP < ( ((/VB.?/ < be) $+ (VP <, VBN)) $++ NP | $++ PP)) | < (((/VB.?/ < be) $+ (VP <, VBN))  $+ (S < (NP $++ VP)) ) | < (((/VB.?/ < be) $+ (VP <, VBN))  $+ (S < (VP < (TO $+ VP) ))) | < (((/VB.?/ < be) $+ (VP <, VBN))  $++ (VP < /VB?/)) | < (((/VB.?/ < be) $+ (VP <, VBN)) $+ (SBAR < ((IN < that) $+ S)))| < (((/VB.?/ < be) $+ (VP <, VBN))   ($+( ADJP < (JJ $+ (S< (VP < ( TO $+ VP | $+ /VB.?/) )))))) | < (((/VB.?/ < be) $+ (VP <, VBN))  $+ (ADJP < JJ)  $++ (S < (VP < (TO $+ VP)))) | < ( ((/VB.?/ < be) $+ (VP <, VBN))  ($+ JJ $++ S) )) )) ) >, S & !>> /^(SBAR)$/");
        patternsExtractionSR.put("sysrespPassiveVoice3","((NP=nounPhrase < /\\b(NN.?|NP|PRP)\\b/) $+ (VP=verbPhrase  < ( MD ?$+ ADVP $++ (/VB.?/ < be) $++ (VP  < (VBN $++ NP | $++ PP) | < ((VP <, VBN) $++ NP | $++ PP) | < (VBN  $+ (S < (NP $++ VP)) ) |  < (VBN  $+ (S < (VP < (TO $+ VP) ))) | < (VBN  $++ (VP < /VB?/)) | < (VBN $+ (SBAR < ((IN < that) $+ S))) | < (VBN  ($+( ADJP < (JJ $+ (S< (VP < ( TO $+ VP | $+ /VB.?/) )))))) | < (VBN  $+ (ADJP < JJ)  $++ (S < (VP < (TO $+ VP)))) | < (VBN  ($+ JJ $++ S) )) ))) > S & !>> /^(SBAR)$/ &$-- /[A-Za-z]/");
        return patternsExtractionSR;
    }

    //Tregex patterns to identify passive voice in the condition
    public static HashMap<String,String> loadPatternsPassiveVoice()
    {
        HashMap<String, String> patternsExtraction = new HashMap<String, String>();

        patternsExtraction.put("S1PassiveVoice1","(/WH.?/ $+ (S <, (S < (/N.?/ $+ (VP <, ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))) )) ))) > SBAR &!>> VP");
        patternsExtraction.put("S1PassiveVoice2","(/WH.?/ $+ (S <, (S < (/N.?/ $+ (VP <, ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] ))  )) ))) > SBAR &!>> VP");

        patternsExtraction.put("S2PassiveVoice1","(SBAR < (WHADVP $+ (S < (/N.?/ $+ (VP <, ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based)))))  ))) > (S > SBAR) & > !VP");
        patternsExtraction.put("S2PassiveVoice2","(SBAR < (WHADVP $+ (S < (/N.?/ $+ (VP <, ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] ))))  ))) > (S > SBAR) & > !VP");

        patternsExtraction.put("S3PassiveVoice1", "((IN=conjunction < /^(Once|once)$/)  $+ (S=clause < (/N.?/ $+ (VP <, ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based)))))  )) [> SBAR | > S] & > !VP");
        patternsExtraction.put("S3PassiveVoice2", "((IN=conjunction < /^(Once|once)$/)  $+ (S=clause < (/N.?/ $+ (VP <, ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] ))))  )) [> SBAR | > S] & > !VP");

        //After SR
        patternsExtraction.put("S4PassiveVoice1","((SBAR < ((S < (/N.?/ $+ (VP <, ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))))) ) [$- (IN !< that) |  $- WHADVP | $- WRB ])) $-- NP) >> (((VP < (MD ?$+ ADVP)) $- (NP [< /NN.?/|< NP|< PRP])) >> S)");
        patternsExtraction.put("S4PassiveVoice2","((SBAR < ((S < (/N.?/ $+ (VP <, ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] )))) ) [$- (IN !< that) |  $- WHADVP | $- WRB ])) $-- NP) >> (((VP < (MD ?$+ ADVP)) $- (NP [< /NN.?/|< NP|< PRP])) >> S)");
        patternsExtraction.put("S5PassiveVoice1","(/(IN|WHADVP|WRB)/  $+ (S < (/N.?/ $+ (VP <, ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based)))))  ) !>> /(VP|SBAR)/)");
        patternsExtraction.put("S5PassiveVoice2","(/(IN|WHADVP|WRB)/  $+ (S < (/N.?/ $+ (VP <, ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] ))))  )  !>> /(VP|SBAR)/)");
        patternsExtraction.put("S6PassiveVoice1","((/(IN|WHADVP|WRB)/=conjunction !< /(of|to)/)  $+ (/N.?/ $+ (VP <, ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based)))))   !>> /(VP|SBAR)/)");
        patternsExtraction.put("S6PassiveVoice2","((/(IN|WHADVP|WRB)/=conjunction !< /(of|to)/)  $+ (/N.?/ $+ (VP <, ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] ))))   !>> /(VP|SBAR)/)");

        //Common
        patternsExtraction.put("S7PassiveVoice1","(SBAR < ((/^(IN|WHADVP|WRB)$/ !<< that) $+ (S << (SBAR <<, /(That|that)/) &<<,  (/N.?/ $++ (VP [ < ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))) | < (VP < ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))))]) | $++ ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))))))) !>> /(VP)/");
        patternsExtraction.put("S7PassiveVoice2","(SBAR < ((/^(IN|WHADVP|WRB)$/ !<< that) $+ (S << (SBAR <<, /(That|that)/) &<<,  (/N.?/ $++ (VP [ < ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] )) | < (VP < ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] )))]) | $++ ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] )))))) !>> /(VP)/");
        patternsExtraction.put("S8PassiveVoice1","(SBAR < ((/^(IN|WHADVP|WRB)$/ !<< that) $+ (S !<< SBAR &<<,  (/N.?/ $++ (VP [ < ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))) | < (VP < ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))))]) | $++ ((/VB.?/ < /(is|are|was|were)/) $+ (VP <, (VBN !< based))))))) !>> /(VP)/");
        patternsExtraction.put("S8PassiveVoice2","(SBAR < ((/^(IN|WHADVP|WRB)$/ !<< that) $+ (S !<< SBAR &<<,  (/N.?/ $++ (VP [ < ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] )) | < (VP < ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] )))]) | $++ ((VBZ < /(has|have)/) $++ (VP [< ((VBN < been)   $+ (VP <, VBN)) | < (VP <, ((VBN < been) $+ (VP <, VBN)))] )) | $++ (VP <  (((/VB*/ < /(has|have)/) $++ (VP [  < ((VBN < been)   $+ (VP <, VBN)) |    < (VP <, ((VBN < been) $+ (VP <, VBN)))  | < (ADVP $+  VBN)  | < (ADVP $+  (VP < VBN ))   ])))))))) !>> /(VP)/"); // 29.11_2

        return patternsExtraction;
    }
}
