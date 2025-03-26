// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

import "./contracts/token/ERC20/ERC20.sol";
import "./Blacklist.sol";

// DEPCOIN
// 1. the balance of each account should be non-negative
// 2. the state of the accounts cannot be modified by unauthorized users
// 3. the system should guarantee the non-repudiation of all operations
// issued on an account

// ISTCOIN
//  ✔   1. its symbol shall be “IST”
//  ✔   2. contain 2 decimals
//  ✔   3. total supply of 100 million units
//      4. When performing a
//      transfer or transferFrom, the ERC-20 contract shall call the access
//      control contract (???) to check whether the client account address is allowed
//      to transfer

contract ISTCoin is ERC20 {
    uint internal constant TOKEN_SUPPLY = 100_000_000;
    Blacklist private _blacklist;

    // 1., 3.
    constructor() ERC20("ISTCoin", "IST") {
        _blacklist = new Blacklist();
        _mint(_msgSender(), TOKEN_SUPPLY * 10 ** uint256(decimals()));
    }

    // TODO -> check: tudo isto já está no ERC20.sol
    //mapping(address account => uint256) private _balances;
    //mapping(address account => mapping(address spender => uint256)) private _allowances;
    //uint256 private _totalSupply;
    //string private _name;
    //string private _symbol;

    // quando tinha view queixava-se: "Function state mutability can be restricted to pure"
    function decimals() public override pure returns (uint8) {
        return 2;
    }

    function transfer(address _to, uint256 _value) public override returns (bool success) {
        // acl check for message sender
        address owner = _msgSender();
        require (!_blacklist.isBlacklisted(owner), "ISTCoin: sender is blacklisted and cannot transfer");
        // TODO -> add acl check to _to ??
        _transfer(owner, _to, _value);
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public override returns (bool success) {
        // acl check for _from
        require (!_blacklist.isBlacklisted(_from), "ISTCoin: spender is blacklisted and cannot transfer");
        // TODO -> add acl check to _to (spender) ??
        address spender = _msgSender();
        _spendAllowance(_from, spender, _value);
        _transfer(_from, _to, _value);
        return true;
    }

}
