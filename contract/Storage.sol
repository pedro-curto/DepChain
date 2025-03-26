pragma solidity ^0.8.26;

contract Storage {
    uint256 private count;
    mapping(address => uint256) private numbers;

    function update(uint256 number) public {
        numbers[msg.sender] = number;
        count += 1;
    }

    function retrieve_count() public view returns (uint256) {
        return count;
    }

    function retrieve_number() public view returns (uint256) {
        return numbers[msg.sender];
    }
}