import 'package:flutter/material.dart';
import 'package:nxp/utils/ExapmleNames.dart';
import 'package:meta/meta.dart';

/// A [CategoryItem] to display a [CategoryName].
class CategoryItem extends StatelessWidget {
  final CategoryName categoryName;
  final ValueChanged<CategoryName> onTap;

  const CategoryItem({
    Key key,
    @required this.categoryName,
    this.onTap,
  })
      : assert(categoryName != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
      elevation: 4.0,
      child: Container(
        decoration: BoxDecoration(
          border: Border(
            left: BorderSide(
              width: 4.0,
              color: Colors.lightGreen,
            ),
          ),
        ),
        child: InkWell(
          onTap: () {
            Navigator.pushNamed(context, "/${categoryName.title}");
          },
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisSize: MainAxisSize.max,
            children: <Widget>[
              Expanded(
                child: Container(
                  margin: EdgeInsets.symmetric(vertical: 16.0, horizontal: 8.0),
                  child: Text(
                    categoryName.title,
                    softWrap: true,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: Icon(
                  Icons.chevron_right,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

