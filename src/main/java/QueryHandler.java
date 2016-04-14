import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class QueryHandler {
    public interface ResponseValidator {
        boolean isPolluted(Record question, Record[] cleanRecords, Record[] dirtyRecords);
    }

    private class LookupTask implements Callable<Integer> {
        private Lookup lookup;

        public LookupTask(Lookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public Integer call() throws Exception {
            this.lookup.run();
            return this.lookup.getResult();
        }
    }

    private static Logger logger = LoggerFactory.getLogger(QueryHandler.class);
    private final ArrayList<ResponseValidator> validators;

    private Cache cleanCache, dirtyCache;
    private Resolver cleanResolver, dirtyResolver;

    private ExecutorService taskExecutor;

    public QueryHandler(String cleanRemoteAddress, String dirtyRemoteAddress) throws UnknownHostException {
        this.cleanCache = new Cache();
        this.cleanResolver = new SimpleResolver(cleanRemoteAddress);
        this.dirtyCache = new Cache();
        this.dirtyResolver = new SimpleResolver(dirtyRemoteAddress);

        this.taskExecutor = Executors.newCachedThreadPool();

        this.validators = new ArrayList<>();
    }

    @Override
    protected void finalize() throws Throwable {
        this.taskExecutor.shutdownNow();
        super.finalize();
    }

    public void addValidator(ResponseValidator validator) {
        this.validators.add(validator);
    }

    public Message handle(Message clientQuery) {
        int id = clientQuery.getHeader().getID();
        Record clientQuestion = clientQuery.getQuestion();

        Message response = new Message(id);
        Header responseHeader = response.getHeader();

        if (clientQuestion != null) {
            logger.debug("MSG {} LOOKUP {}", id, clientQuestion.toString());

            // lookup clean source
            Lookup cleanLookup = new Lookup(clientQuestion.getName(), clientQuestion.getType(), clientQuestion.getDClass());
            cleanLookup.setCache(this.cleanCache);
            cleanLookup.setResolver(this.cleanResolver);
            FutureTask<Integer> cleanLookupFuture = new FutureTask<>(new LookupTask(cleanLookup));
            this.taskExecutor.submit(cleanLookupFuture);

            // lookup clean source
            Lookup dirtyLookup = new Lookup(clientQuestion.getName(), clientQuestion.getType(), clientQuestion.getDClass());
            dirtyLookup.setCache(this.dirtyCache);
            dirtyLookup.setResolver(this.dirtyResolver);
            FutureTask<Integer> dirtyLookupFuture = new FutureTask<>(new LookupTask(dirtyLookup));
            this.taskExecutor.submit(dirtyLookupFuture);

            // retrieve FutureTask results
            try {
                int cleanLookupResult = cleanLookupFuture.get();
                int dirtyLookupResult = dirtyLookupFuture.get();

                int lookupResult;
                if ((lookupResult = cleanLookupResult) == Lookup.SUCCESSFUL)
                    if ((lookupResult = dirtyLookupResult) == Lookup.SUCCESSFUL) {
                        Record[] cleanAnswers = cleanLookup.getAnswers();
                        Record[] dirtyAnswers = dirtyLookup.getAnswers();

                        // identify fake domains
                        if (cleanAnswers != null) {
                            Record[] selectedAnswers;

                            if (dirtyAnswers != null) {
                                // identify pollution
                                if (this.isPolluted(clientQuestion, cleanAnswers, dirtyAnswers)) {
                                    selectedAnswers = cleanAnswers;
                                    logger.debug("MSG {} SELECT CLEAN", id);
                                } else {
                                    selectedAnswers = dirtyAnswers;
                                    logger.debug("MSG {} SELECT DIRTY", id);
                                }
                            } else
                                selectedAnswers = cleanAnswers;

                            // copy records
                            for (Record rec : selectedAnswers)
                                response.addRecord(rec, Section.ANSWER);
                        }
                    }

                // translate lookup result
                switch (lookupResult) {
                    case Lookup.SUCCESSFUL:
                    case Lookup.TYPE_NOT_FOUND:
                        responseHeader.setRcode(Rcode.NOERROR);
                        break;
                    case Lookup.HOST_NOT_FOUND:
                        responseHeader.setRcode(Rcode.NXDOMAIN);
                        break;
                    case Lookup.TRY_AGAIN:
                    case Lookup.UNRECOVERABLE:
                        responseHeader.setRcode(Rcode.SERVFAIL);
                        break;
                }
            } catch (ExecutionException e) {
                logger.error("MSG {} LOOKUP FAILED: {}", id, e.toString());
                responseHeader.setRcode(Rcode.SERVFAIL);
            } catch (InterruptedException e) {
                logger.warn("MSG {} LOOKUP INTERRUPTED", id);
                responseHeader.setRcode(Rcode.SERVFAIL);
            }
        } else {
            logger.debug("MSG {} LOOKUP NULL");
            responseHeader.setRcode(Rcode.NOERROR);
        }

        response.setHeader(responseHeader);
        return response;
    }

    private boolean isPolluted(Record clientQuestion, Record[] cleanAnswers, Record[] dirtyAnswers) {
        for (ResponseValidator validator : this.validators)
            if (validator.isPolluted(clientQuestion, cleanAnswers, dirtyAnswers))
                return true;
        return false;
    }
}
