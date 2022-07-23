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

package snw.kookbc.impl.pageiter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Invitation;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.InvitationImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class ChannelInvitationIterator extends PageIteratorImpl<Set<Invitation>> {
    private final Channel channel;

    private Set<Invitation> result;

    public ChannelInvitationIterator(Channel channel) {
        this.channel = channel;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?channel_id=%s", HttpAPIRoute.INVITE_LIST.toFullURL(), channel.getId());
    }

    @Override
    protected void processElements(JsonArray array) {
        result = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            Guild guild = KBCClient.getInstance().getStorage().getGuild(object.get("guild_id").getAsString());
            String urlCode = object.get("url_code").getAsString();
            String url = object.get("url").getAsString();
            User master = KBCClient.getInstance().getStorage().getUser(object.getAsJsonObject("user").get("id").getAsString());
            result.add(new InvitationImpl(
                    guild, channel, urlCode, url, master
            ));
        }
    }

    @Override
    protected void onHasNextButNoMoreElement() {
        result = null;
    }

    @Override
    public Set<Invitation> next() {
        if (result == null) {
            throw new NoSuchElementException();
        }
        return Collections.unmodifiableSet(result);
    }
}
