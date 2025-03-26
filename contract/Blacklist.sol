// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

import "./contracts/token/ERC20/ERC20.sol";

contract Blacklist {

    mapping (address => bool) private _blacklist;
    address public _owner;

    event Blacklisted(address indexed account);
    event UnBlacklisted(address indexed account);

    modifier onlyOwner() {
        require(msg.sender == _owner, "Ownable: caller is not the owner");
        _;
    }

    constructor() {
        _owner = msg.sender;
    }

    // blacklist and unblacklist are limited to the owner
    function addToBlacklist(address account) public onlyOwner {
        _blacklist[account] = true;
        emit Blacklisted(account);
    }

    function removeFromBlacklist(address account) public onlyOwner {
        _blacklist[account] = false;
        emit UnBlacklisted(account);
    }

    function isBlacklisted(address account) public view returns (bool) {
        return _blacklist[account];
    }

}
