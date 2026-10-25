from src import pragyan as P


def test_kappa():
    assert P.cohen_kappa([True, False, True, False], [True, False, True, False]) == 1.0
    assert abs(P.cohen_kappa([True, True, False, False], [True, False, True, False])) < 1e-9


def test_cl_codes_cover_every_question():
    corpus = P.load_corpus()
    for rid, codes in P.CL_CODES.items():
        assert len(codes) == sum(len(i["questions"]) for i in corpus[rid]["items"]), rid
        assert all(c in P.CODES for c in codes)
