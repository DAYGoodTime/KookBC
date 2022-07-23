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

import snw.jkook.entity.User;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Map;

public class PrivateMessageImpl extends MessageImpl implements PrivateMessage {
    public PrivateMessageImpl(String id, User user, BaseComponent component, long timeStamp) {
        super(id, user, component, timeStamp);
    }

    @Override
    public void delete() {
        if (KBCClient.getInstance().getBot().getUser() == getSender()) {
            Map<String, Object> body = new MapBuilder()
                    .put("msg_id", getId())
                    .build();
            KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.USER_CHAT_MESSAGE_DELETE.toFullURL(), body);
        } else {
            throw new UnsupportedOperationException(); // You can't delete others' pm to you
        }
    }
}
