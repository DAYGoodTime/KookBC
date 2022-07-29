/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

public abstract class MessageImpl implements Message {
    private final String id;
    private final User user;
    private final BaseComponent component;
    private final long timeStamp;
    private final Message quote;

    public MessageImpl(String id, User user, BaseComponent component, long timeStamp, Message quote) {
        this.id = id;
        this.user = user;
        this.component = component;
        this.timeStamp = timeStamp;
        this.quote = quote;
    }

    @Override
    public BaseComponent getComponent() {
        return component;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public @Nullable Message getQuote() {
        return quote;
    }

    @Override
    public User getSender() {
        return user;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public Collection<User> getUserByReaction(CustomEmoji customEmoji) {
        JsonArray array;
        try {
            String rawStr = KBCClient.getInstance().getNetworkClient().getRawContent(
                    String.format(
                            "%s?msg_id=%s&emoji=%s",
                            ((this instanceof TextChannelMessage) ?
                                    HttpAPIRoute.CHANNEL_MESSAGE_REACTION_LIST :
                                    HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_LIST)
                                    .toFullURL(),
                            getId(),
                            customEmoji.getId().contains("/") ?
                                    customEmoji.getId() :
                                    URLEncoder.encode(customEmoji.getId(), "UTF-8")
                    )
            );
            array = JsonParser.parseString(rawStr).getAsJsonObject().getAsJsonArray("data");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("40300")) { // 40300, so we should throw IllegalStateException
                throw new IllegalStateException();
            } else {
                throw e;
            }
        } catch (UnsupportedEncodingException e) {
            throw new Error("No UTF-8 charset! Please check your JVM installation.", e);
        }
        Collection<User> result = new ArrayList<>();
        for (JsonElement element : array) {
            result.add(KBCClient.getInstance().getStorage().getUser(element.getAsJsonObject().get("id").getAsString()));
        }
        return result;
    }
}
