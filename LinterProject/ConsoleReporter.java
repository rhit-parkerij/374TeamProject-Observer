public class ConsoleReporter {
    public void report(List<LintIssue> issues){
        //TODO: implement console reporting logic
        for(LintIssue issue : issues){
            System.out.println(issue);
        }
    }
}
