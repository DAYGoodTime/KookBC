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

package snw.kookbc.impl.entity;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Map;
import java.util.Objects;

public class CustomEmojiImpl implements CustomEmoji {
    private final String id;
    private final Guild guild;
    private String name;

    public CustomEmojiImpl(String id, String name, Guild guild) {
        this.id = id;
        this.name = name;
        // optional attributes are following:
        this.guild = guild;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Map<String, Object> body = new MapBuilder()
                .put("id", getId())
                .put("name", name)
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_EMOJI_UPDATE.toFullURL(), body);
        this.name = name;
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    @Override
    public void delete() {
        Map<String, Object> body = new MapBuilder()
                .put("id", getId())
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_EMOJI_DELETE.toFullURL(), body);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomEmojiImpl)) return false;
        CustomEmojiImpl that = (CustomEmojiImpl) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, guild, name);
    }

    public void setName0(String name) {
        this.name = name;
    }
}
