package uk.co.causebrook.eubot.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexListener implements MessageListener {
    private Pattern p;
    private RegexHandler h;
    private MessageListener mL;
    
    public RegexListener(Pattern pattern, RegexHandler handler) {
        p = pattern;
        h = handler;
    }
    
    public RegexListener(String regex, RegexHandler handler) throws PatternSyntaxException {
        p = Pattern.compile(regex);
        h = handler;
    }

    public RegexListener(Pattern pattern, MessageListener listener) {
        p = pattern;
        mL = listener;
    }

    public RegexListener(String regex, MessageListener listener) throws PatternSyntaxException {
        p = Pattern.compile(regex);
        mL = listener;
    }
    
    @Override
    public void onPacket(MessageEvent<?> e) {
        Matcher m = p.matcher(e.getContent());
        if(m.matches()) {
            if(h != null) h.onRegex(e, m);
            else if(mL != null) mL.onPacket(e);
        }
    }
    
    @FunctionalInterface
    public interface RegexHandler {
        void onRegex(MessageEvent<?> e, Matcher m);
    }
}
