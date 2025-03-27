pragma solidity ^0.8.26;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

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
    //Blacklist private _blacklist;

    uint internal constant TOKEN_SUPPLY = 100_000_000;
    mapping (address => bool) private _blacklist;
    address public _blacklistOwner;

    // from Blacklist
    event Blacklisted(address indexed account);
    event UnBlacklisted(address indexed account);
    modifier onlyOwner() {
        require(msg.sender == _blacklistOwner,
            "Ownable: caller is not the owner");
        _;
    }

    // 1., 3.
    constructor() ERC20("ISTCoin", "IST") {
        //_blacklist = new Blacklist();
        _blacklistOwner = msg.sender;
        _mint(_msgSender(), TOKEN_SUPPLY * 10 ** uint256(decimals()));
    }

    // quando tinha view queixava-se: "Function state mutability can be restricted to pure"
    function decimals() public override pure returns (uint8) {
        return 2;
    }

    function transfer(address _to, uint256 _value) public override returns (bool success) {
        // acl check for message sender
        address owner = _msgSender();
        require (!isBlacklisted(owner), "ISTCoin: sender is blacklisted and cannot transfer");
        // TODO -> add acl check to _to ??
        _transfer(owner, _to, _value);
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public override returns (bool success) {
        // acl check for _from
        require (!isBlacklisted(_from), "ISTCoin: spender is blacklisted and cannot transfer");
        // TODO -> add acl check to _to (spender) ??
        address spender = _msgSender();
        _spendAllowance(_from, spender, _value);
        _transfer(_from, _to, _value);
        return true;
    }

    // from Blacklist
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
