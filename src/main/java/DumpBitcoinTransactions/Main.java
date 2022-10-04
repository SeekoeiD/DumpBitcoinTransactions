package DumpBitcoinTransactions;

import java.io.*;
import java.util.*;
import org.json.*;
import okhttp3.*;

public class Main {

    public static void main(String[] args) {
        try {
            JSONObject getblockchaininfo = run_rpc("getblockchaininfo", new JSONArray());

            String error = getblockchaininfo.optString("error");
            if (error.length() != 0) {
                throw new Exception(error);
            }

            JSONObject output = getblockchaininfo.getJSONObject("output");
            int blocks = output.getInt("blocks");
            int pruneheight = output.optInt("pruneheight", 0);

            System.out.println("blocks: " + blocks);
            System.out.println("pruneheight: " + pruneheight);

            int blocks_saved = 0, blocks_aim = 250;
            int tx_saved = 0, tx_aim = 100000;

            for (int block = pruneheight; block <= blocks; block++) {
                System.out.println(block + "/" + blocks);

                JSONObject getblockhash = run_rpc("getblockhash", new JSONArray().put(block));
                String blockhash = getblockhash.getString("output");

                if (blocks_saved < blocks_aim) {
                    JSONObject getblock1 = run_rpc("getblock", new JSONArray().put(blockhash).put(1));
                    write_block(getblock1.getJSONObject("output").toString());
                    blocks_saved++;
                }

                if (tx_saved < tx_aim) {
                    JSONObject getblock2 = run_rpc("getblock", new JSONArray().put(blockhash).put(2));
                    JSONArray tx = getblock2.getJSONObject("output").getJSONArray("tx");

                    for (int q = 0; q < tx.length(); q++) {
                        if (tx_saved < tx_aim) {
                            write_tx(tx.getJSONObject(q).toString());
                            tx_saved++;
                        }
                    }
                }

                if (blocks_saved == blocks_aim && tx_saved == tx_aim) {
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write_block(String block) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter("blocks.txt", true));

            w.write(block);
            w.newLine();
            w.close();
        } catch (Exception e) {
        }
    }

    public static void write_tx(String tx) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter("transactions.txt", true));

            w.write(tx);
            w.newLine();
            w.close();
        } catch (Exception e) {
        }
    }

    public static JSONObject run_rpc(String cmd, JSONArray params) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .protocols(Arrays.asList(Protocol.HTTP_1_1))
                .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        String credential = Credentials.basic("user", "pass");
                        return response.request().newBuilder().header("Authorization", credential).build();
                    }
                })
                .build();

        //{ "method": "getinfo", "params": [], "id": 1}
        JSONObject b = new JSONObject();
        b.put("method", cmd);
        b.put("params", params);

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, b.toString());

        Request request = new Request.Builder()
                .url("http://user:pass@192.168.100.208:8332") //mainnet
                //.url("http://user:pass@192.168.100.203:18332 ") //testnet
                .addHeader("Content-Type", "text/plain")
                .method("POST", body)
                .build();

        Response response = client.newCall(request).execute();
        BufferedReader r = new BufferedReader(new InputStreamReader(response.body().byteStream()));

        String answer = r.readLine();

        //{"result":0.00000000,"error":null,"id":"curltest"}
        //{"result":null,"error":{"code":-1, "message":"..."},"id":"curltest"}
        JSONObject j = new JSONObject(answer);

        JSONObject ret = new JSONObject();
        ret.put("output", j.get("result"));
        ret.put("error", (j.optJSONObject("error") == null ? "" : j.optJSONObject("error").optString("message")));

        return ret;
    }
}
