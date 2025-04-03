package depchain.common.domain;

public class ContractData {
    private String address;
    private String deploymentBytecode;
    private String runtimeBytecode;
    private String ownerAddress;

    public ContractData(String address, String deploymentBytecode, String runtimeBytecode, String ownerAddress) {
        this.address = address;
        this.deploymentBytecode = deploymentBytecode;
        this.runtimeBytecode = runtimeBytecode;
        this.ownerAddress = ownerAddress;
    }

    public String getAddress() {
        return address;
    }

    public String getDeploymentBytecode() {
        return deploymentBytecode;
    }

    public String getRuntimeBytecode() {
        return runtimeBytecode;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }

    @Override
    public String toString() {
        return "ContractData{" +
                "address='" + address + '\'' +
                ", deploymentBytecode='" + deploymentBytecode + '\'' +
                ", runtimeBytecode='" + runtimeBytecode + '\'' +
                ", ownerAddress='" + ownerAddress + '\'' +
                '}';
    }

}
