package eu.nextdata.ofsoapbox;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.dom4j.DocumentException;
import okhttp3.OkHttpClient;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.net.*;

public class SoapboxPlugin implements Plugin, PacketInterceptor {
    private final OkHttpClient httpClient = new OkHttpClient();
    private InterceptorManager interceptorManager;

    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        interceptorManager = InterceptorManager.getInstance();
        interceptorManager.addInterceptor(this);
    }

    @Override
    public void destroyPlugin() {
        interceptorManager.removeInterceptor(this);
    }

    @Override
    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
        if (processed) return;
        if (session == null) return;
        if (session.getStatus() != Session.STATUS_AUTHENTICATED) return;

        if (packet instanceof Message) {
            String from = packet.getFrom().getNode();
            if (!from.startsWith("sbrw.")) return;
            int pid;
            try {
                pid = Integer.valueOf(from.split("\\.")[1]);
            } catch (NumberFormatException e) {
                return;
            }

            String body = ((Message) packet).getBody();
            Element chatMsg;
            try {
                chatMsg = DocumentHelper.parseText(body).getRootElement();
            } catch (DocumentException e) {
                return;
            }

            if (!chatMsg.getName().equals("ChatMsg")) return;
            String text = chatMsg.elementText("Msg");
            if (text == null) return;
            if (text.startsWith("/")) {
                try {
                    HttpUrl url = HttpUrl.parse(JiveGlobals.getProperty("plugin.soapbox.url")).newBuilder()
                            .addQueryParameter("pid", String.valueOf(pid))
                            .addQueryParameter("cmd", text)
                            .build();
                    Request request = new Request.Builder()
                            .addHeader("Authorization", JiveGlobals.getProperty("plugin.soapbox.secret"))
                            .url(url)
                            .post(RequestBody.create(new byte[0]))
                            .build();
                    httpClient.newCall(request).execute().close();
                } catch (IOException | NullPointerException e) {}
                throw new PacketRejectedException();
            }
        }
    }
}