package uk.co.causebrook.eubot.packets.replies;

import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.fields.SessionView;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class WhoReply extends Data {
    private ArrayList<SessionView> listing;

    public ArrayList<SessionView> getListing() { return listing; }
}
