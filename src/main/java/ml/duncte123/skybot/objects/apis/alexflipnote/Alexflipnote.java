/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017 - 2018  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ml.duncte123.skybot.objects.apis.alexflipnote;

import com.github.natanbc.reliqua.request.PendingRequest;
import com.google.gson.Gson;
import me.duncte123.botCommons.web.WebUtils;
import me.duncte123.botCommons.web.WebUtilsErrorUtils;
import me.duncte123.weebJava.helpers.QueryBuilder;
import okhttp3.Request;
import org.json.JSONObject;

import java.io.InputStream;

import static me.duncte123.botCommons.web.WebUtils.defaultRequest;

public class Alexflipnote {

    private final Gson gson = new Gson();


    public PendingRequest<FlipnoteColourObj> getRandomColour() {
        return WebUtils.ins.prepareRaw(
                makeRequest("colour/random"),
                (r) -> {
                    JSONObject jsonObject = WebUtilsErrorUtils.toJSONObject(r);
                    jsonObject.put("integer", jsonObject.getInt("int"));
                    return gson.fromJson(jsonObject.toString(), FlipnoteColourObj.class);
                }
        );
    }

    public PendingRequest<InputStream> getAchievement(String text) {
        QueryBuilder builder = new QueryBuilder().append("text", text);
        return WebUtils.ins.prepareRaw(
                makeRequest("achievement" + builder.build()),
                WebUtilsErrorUtils::getInputStream
        );
    }


    private Request makeRequest(String path) {
        return defaultRequest()
                .url("https://api.alexflipnote.xyz/" + path)
                .get()
                .build();
    }
}