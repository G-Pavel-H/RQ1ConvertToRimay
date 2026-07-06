
/*
* Class that manages the conditions that are consecutives within a requirement
* */
public class ConsecCond {
    private Integer start;
    private Integer end;

    public ConsecCond(Integer start, Integer end){
        this.start = start;
        this.end = end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return this.end;
    }

    public Integer getStart() {
        return this.start;
    }
}
