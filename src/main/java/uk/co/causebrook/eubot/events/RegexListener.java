package uk.co.causebrook.eubot.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexListener implements MessageListener {
    private Pattern p;
    private RegexHandler h;
    
    public RegexListener(Pattern pattern, RegexHandler handler) {
        p = pattern;
        h = handler;
    }
    
    public RegexListener(String regex, RegexHandler handler) throws PatternSyntaxException {
        p = Pattern.compile(regex);
        h = handler;
    }
    
    @Override
    public void onPacket(MessageEvent e) {
        Matcher m = p.matcher(e.getContent());
        if(m.matches()) h.onRegex(e, m);
    }
    
    @FunctionalInterface
    public interface RegexHandler {
        void onRegex(MessageEvent e, Matcher m);
    }
}
