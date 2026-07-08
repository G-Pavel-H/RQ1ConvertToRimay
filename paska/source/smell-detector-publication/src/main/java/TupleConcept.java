import java.util.ArrayList;
import edu.stanford.nlp.ling.TaggedWord;

public class TupleConcept {
    public String nameConcept;
    public String contentConcept;
    public String pennTreeConcept;
    public ArrayList<TaggedWord> posConcept;

    public TupleConcept(String name, String content)
    {
        this.nameConcept = name;
        this.contentConcept = content;
    }

    public void setContentConcept(String contentConcept) {
        this.contentConcept = contentConcept;
    }

    public void setNameConcept(String nameConcept) {
        this.nameConcept = nameConcept;
    }

    public void setPennTreeConcept(String pennTree) {this.pennTreeConcept = pennTree; }
    public void setPosConcept(ArrayList<TaggedWord> posConc){ this.posConcept = posConc;}


    public String getContentConcept() {
        return contentConcept;
    }

    public String getNameConcept() {
        return nameConcept;
    }

    public String getPennTreeConcept(){return  pennTreeConcept; }

    public ArrayList<TaggedWord> getPosConcept(){ return  posConcept; }
}
