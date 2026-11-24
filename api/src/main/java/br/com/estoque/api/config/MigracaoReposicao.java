package br.com.estoque.api.config;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.MongoNamespace;

/**
 * A colecao de reposicao se chamava "reposicoes"; o enunciado pede "reposicao".
 * Ao iniciar, move os registros antigos para a colecao nova, para quem ja tinha dados
 * no volume do MongoDB. Se nao existir colecao antiga, nao faz nada.
 */
@Component
public class MigracaoReposicao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoReposicao.class);
    static final String ANTIGA = "reposicoes";
    static final String NOVA = "reposicao";

    private final MongoTemplate mongoTemplate;

    public MigracaoReposicao(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mongoTemplate.collectionExists(ANTIGA)) {
            return;
        }
        var antiga = mongoTemplate.getCollection(ANTIGA);
        long total = antiga.countDocuments();

        if (!mongoTemplate.collectionExists(NOVA)) {
            antiga.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), NOVA));
        } else {
            // A nova ja existe (indices criados ou o Worker ja gravou): copia o que falta e apaga a antiga
            List<Document> docs = antiga.find().into(new ArrayList<>());
            var nova = mongoTemplate.getCollection(NOVA);
            for (Document doc : docs) {
                if (nova.countDocuments(new Document("_id", doc.get("_id"))) == 0) {
                    nova.insertOne(doc);
                }
            }
            antiga.drop();
        }
        log.info("Colecao '{}' migrada para '{}' ({} documento(s))", ANTIGA, NOVA, total);
    }
}
