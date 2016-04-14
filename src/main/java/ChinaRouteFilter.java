import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import java.util.ArrayList;
import java.util.List;

public class ChinaRouteFilter implements QueryHandler.ResponseValidator {
    protected static Logger logger = LoggerFactory.getLogger(ChinaRouteFilter.class.getSimpleName());

    protected List<SubnetInfo> subnets;

    public ChinaRouteFilter(List<String> cidrList) {
        this.subnets = new ArrayList<SubnetInfo>(cidrList.size());
        for (String entry : cidrList)
            this.subnets.add((new SubnetUtils(entry)).getInfo());
        logger.info("Loaded {} records", subnets.size());
    }

    public boolean isPolluted(Record question, Record[] cleanResponses, Record[] dirtyResponses) {
        String address;
        for (Record rec : dirtyResponses)
            if (rec.getType() == Type.A) {
                address = rec.rdataToString();
                for (SubnetInfo subnet : this.subnets)
                    if (subnet.isInRange(address)) {
                        logger.debug("DOMAIN {} DIRTY-A {} MATCH {}", question.rdataToString(), address, subnet.getCidrSignature());
                        return false;
                    }
            }
        return true;
    }
}
